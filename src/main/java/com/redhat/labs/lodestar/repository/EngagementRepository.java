package com.redhat.labs.lodestar.repository;

import static com.mongodb.client.model.Aggregates.addFields;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.skip;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.Status;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.filter.SingleFilterOptions;
import com.redhat.labs.lodestar.model.filter.SortOrder;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class EngagementRepository implements PanacheMongoRepository<Engagement> {

    private static final List<String> IMMUTABLE_FIELDS = new ArrayList<>(
            Arrays.asList("uuid", "mongoId", "projectId", "creationDetails", "status", "commits", "launch"));
    private static final String COUNT = "count";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String CASE_INSENSITIVE_QUERY = "(?i)%s";
    private static final String TO_LOWER_QUERY = "$toLower";

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Returns Optional containing an {@link Engagement} that matches the provided
     * subdomain.
     * 
     * @param subdomain
     * @return
     */
    public Optional<Engagement> findBySubdomain(String subdomain) {
        return findBySubdomain(subdomain, Optional.empty());
    }

    public Optional<Engagement> findBySubdomain(String subdomain, Optional<String> engagementUuid) {

        String regex = new StringBuilder("^").append(subdomain).append("$").toString();
        Bson filter = regex("hostingEnvironments.ocpSubDomain", regex, "im");

        if (engagementUuid.isPresent()) {
            filter = and(filter, eq("uuid", engagementUuid.get()));
        }

        return Optional.ofNullable(mongoCollection().find(filter).first());

    }

    /**
     * Returns a {@link List} of {@link Engagement}s where the action attribute is
     * not null.
     * 
     * @return
     */
    public List<Engagement> findByModified() {
        return find("action is not null").list();
    }

    /**
     * A case insensitive string to match against customer names.
     * 
     * @param input
     * @return
     */
    public List<Engagement> findCustomerSuggestions(String input) {

        String queryInput = String.format(CASE_INSENSITIVE_QUERY, input);
        return find("customerName like ?1", queryInput).list();

    }

    /**
     * Returns a {@link List} of {@link Category} matching the
     * {@link ListFilterOptions}. If not options provided, all {@link Category} are
     * returned.
     * 
     * @return
     */
    public List<Category> findCategories(ListFilterOptions options) {

        List<Bson> pipeline = new ArrayList<>();

        // flatten to just categories
        addUnwindStageToPipeline(pipeline, "$categories");

        // filter if suggestion provided
        addMatchStageToPipeline(pipeline, options.getSuggestion(), "categories.name", false);

        // create a field that is the lower case name
        addLowerCaseFieldToPipeline(pipeline, "categories.lower_name", "$categories.name");

        // group and count by lower case name
        addGroupAndCountStageToPipeline(pipeline, "$categories.lower_name");

        // create document with name and count
        addProjectNameAndCountStageToPipeline(pipeline, "name", true);

        // sort
        addSortStageToPipeline(pipeline, options.getSortOrder(), new String[] { COUNT, NAME });

        // paging and limits
        addPagingAndLimitStagesToPipeline(pipeline, options.getPage(), options.getPerPage(), options.getLimit());

        return listFromIterable(mongoCollection().aggregate(pipeline, Category.class));

    }

    private void addUnwindStageToPipeline(List<Bson> pipeline, String fieldName) {
        pipeline.add(unwind(fieldName));
    }

    private void addMatchStageToPipeline(List<Bson> pipeline, Optional<String> suggestion, String fieldName,
            boolean caseSensitive) {

        if (suggestion.isPresent()) {

            if (caseSensitive) {
                pipeline.add(match(regex(fieldName, suggestion.get())));
            } else {
                pipeline.add(match(regex(fieldName, suggestion.get(), "i")));
            }

        }

    }

    private void addLowerCaseFieldToPipeline(List<Bson> pipeline, String tofieldName, String fromFieldName) {

        Document toLowerDocument = new Document(TO_LOWER_QUERY, fromFieldName);
        addFieldStageToPipeline(pipeline, tofieldName, toLowerDocument);

    }

    private void addFieldStageToPipeline(List<Bson> pipeline, String fieldName, Document document) {
        pipeline.add(addFields(new Field<>(fieldName, document)));
    }

    private void addGroupAndCountStageToPipeline(List<Bson> pipeline, String groupByField) {
        BsonField[] fields = new BsonField[] { Accumulators.sum(COUNT, 1) };
        addGroupStageToPipeline(pipeline, groupByField, Optional.of(fields));
    }

    private void addGroupStageToPipeline(List<Bson> pipeline, String fieldName, Optional<BsonField[]> bsonField) {
        if (bsonField.isPresent()) {
            pipeline.add(group(fieldName, bsonField.get()));
        } else {
            pipeline.add(group(fieldName));
        }
    }

    private void addProjectNameAndCountStageToPipeline(List<Bson> pipeline, String nameField, boolean addCount) {
        if (addCount) {
            pipeline.add(project(new Document("_id", 0).append(nameField, "$_id").append(COUNT, "$count")));
        } else {
            pipeline.add(project(new Document().append(nameField, "$_id")));
        }
    }

    private void addSortStageToPipeline(List<Bson> pipeline, Optional<SortOrder> sortOrder, String[] sortFields) {

        if (sortOrder.isPresent() && "asc".equalsIgnoreCase(sortOrder.get().name())) {
            pipeline.add(sort(Sorts.ascending(sortFields)));
        } else {
            pipeline.add(sort(Sorts.descending(sortFields)));
        }

    }

    private void addPagingAndLimitStagesToPipeline(List<Bson> pipeline, Optional<Integer> page,
            Optional<Integer> perPage, Optional<Integer> limit) {

        if (page.isPresent()) {
            Integer pageNumber = page.get();
            Integer pageSize = perPage.isPresent() ? perPage.get() : 20;
            pipeline.add(skip(pageSize * (pageNumber - 1)));
            pipeline.add(limit(pageSize));

        } else if (limit.isPresent()) {
            pipeline.add(limit(limit.get()));
        }

    }

    private <T> List<T> listFromIterable(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }

    /**
     * Returns all artifact types that match the provides {@link String}.
     * 
     * @param input
     * @return
     */
    public List<String> findArtifactTypes(ListFilterOptions options) {

        List<Bson> pipeline = new ArrayList<>();

        // flatten to just categories
        addUnwindStageToPipeline(pipeline, "$artifacts");

        // filter if suggestion provided
        addMatchStageToPipeline(pipeline, options.getSuggestion(), "artifacts.type", false);

        // create a field that is the lower case name
        addLowerCaseFieldToPipeline(pipeline, "artifacts.lower_type", "$artifacts.type");

        // group by lower case name
        addGroupStageToPipeline(pipeline, "$artifacts.lower_type", Optional.empty());

        // project
        addProjectNameAndCountStageToPipeline(pipeline, TYPE, false);

        // sort
        addSortStageToPipeline(pipeline, options.getSortOrder(), new String[] { TYPE });

        // paging and limits
        addPagingAndLimitStagesToPipeline(pipeline, options.getPage(), options.getPerPage(), options.getLimit());

        return listFromIterable(mongoCollection().aggregate(pipeline, Artifact.class)).stream().map(Artifact::getType)
                .collect(Collectors.toList());

    }

    /**
     * Returns all unique artifact types.
     * 
     * @return
     */
//    public List<String> findAllArtifactTypes() {
//
//        // get all unique artifact types
//        Iterable<Artifact> iterable = mongoCollection().aggregate(Arrays.asList(unwind("$artifacts"),
//                addFields(new Field<>("artifacts.lower_type", new Document(TO_LOWER_QUERY, "$artifacts.type"))),
//                group("$artifacts.lower_type"), project(new Document().append("type", "$_id")),
//                sort(Sorts.ascending("type"))), Artifact.class);
//
//        return StreamSupport.stream(iterable.spliterator(), false).map(Artifact::getType).collect(Collectors.toList());
//
//    }

    /**
     * Returns an {@link Optional} containing the updated {@link Engagement} where
     * the uuid matched. Otherwise, returns an empty {@link Optional}.
     * 
     * @param uuid
     * @param projectId
     * @return
     */
    public Optional<Engagement> setProjectId(String uuid, Integer projectId) {

        Bson filter = eq("uuid", uuid);
        Bson update = set("projectId", projectId);

        FindOneAndUpdateOptions optionAfter = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

        return Optional.ofNullable(this.mongoCollection().findOneAndUpdate(filter, update, optionAfter));

    }

    /**
     * Sets the {@link Status} for the given UUID.
     * 
     * @param uuid
     * @param status
     * @return
     */
    public Optional<Engagement> setStatus(String uuid, Status status) {

        Bson filter = eq("uuid", uuid);
        Bson update = set("status", status);

        FindOneAndUpdateOptions optionAfter = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

        return Optional.ofNullable(this.mongoCollection().findOneAndUpdate(filter, update, optionAfter));

    }

    /**
     * Sets the {@link Commit}s for the given UUID.
     * 
     * @param uuid
     * @param commits
     * @return
     */
    public Optional<Engagement> setCommits(String uuid, List<Commit> commits) {

        Bson filter = eq("uuid", uuid);
        Bson update = set("commits", commits);

        FindOneAndUpdateOptions optionAfter = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

        return Optional.ofNullable(this.mongoCollection().findOneAndUpdate(filter, update, optionAfter));

    }

    /**
     * Returns an {@link Optional} containing the updated {@link Engagement} where
     * last update matched. Otherwise, returns an empty {@link Optional}
     * 
     * @param replacement
     * @param lastUpdate
     * @param skipLaunch
     * @return
     */
    public Optional<Engagement> updateEngagementIfLastUpdateMatched(Engagement toUpdate, String lastUpdate,
            Boolean skipLaunch) {

        // create the bson for filter and update
        Bson filter = createFilterForEngagement(toUpdate, lastUpdate);
        Bson update = createUpdateDocument(toUpdate, skipLaunch);

        FindOneAndUpdateOptions optionAfter = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

        return Optional.ofNullable(this.mongoCollection().findOneAndUpdate(filter, update, optionAfter));

    }

    /**
     * Returns a {@link Bson} containing the filter to find {@link Engagement} with
     * the corresponding customer name, project name, and last update timestamp.
     * 
     * @param engagement
     * @param lastUpdate
     * @return
     */
    private Bson createFilterForEngagement(Engagement engagement, String lastUpdate) {
        return and(eq("uuid", engagement.getUuid()), eq("lastUpdate", lastUpdate));
    }

    /**
     * Returns a {@link Bson} containing the fields to be updated for a given
     * {@link Engagement}.
     * 
     * @param engagement
     * @param skipLaunch
     * @return
     */
    private Bson createUpdateDocument(Engagement engagement, boolean skipLaunch) {

        Bson updates = null;

        // convert to map
        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
        };
        Map<String, Object> fieldMap = objectMapper.convertValue(engagement, typeRef);

        // remove values that should not be updated
        IMMUTABLE_FIELDS.forEach(f -> {
            if (!f.equals("launch") || skipLaunch) {
                fieldMap.remove(f);
            }
        });

        // add a set for each field in the update
        for (Entry<String, Object> entry : fieldMap.entrySet()) {

            Bson update = set(entry.getKey(), entry.getValue());

            if (null == updates) {
                updates = update;
            } else {
                updates = combine(updates, update);
            }

        }

        return updates;

    }

    /**
     * Returns a {@link List} of {@link Engagement}s that contain the supplied
     * categories.
     * 
     * If FilterOptions is provided, the associated projection will be used.
     * Otherwise, all fields will be returned.
     * 
     * @param categories
     * @param filterOptions
     * @return
     */
    public List<Engagement> findByCategories(String categories, SingleFilterOptions filterOptions) {

        // split the list
        Set<String> categorySet = Stream.of(categories.split(",")).collect(Collectors.toSet());

        // create regex for each category
        List<Bson> regexs = categorySet.stream().map(category -> regex("categories.name", category, "i"))
                .collect(Collectors.toList());

        // or the regexs together
        Bson bson = or(regexs);
        return findAll(Optional.of(bson), filterOptions);

    }

    /**
     * Returns an {@link Optional} containing the {@link Engagement} with the UUID.
     * Otherwise, an empty {@link Optional} is returned.
     * 
     * @param uuid
     * @return
     */
    public Optional<Engagement> findByUuid(String uuid) {
        return findByUuid(uuid, new SingleFilterOptions());
    }

    /**
     * Returns an {@link Optional} containing the {@link Engagement} with the UUID.
     * Otherwise, an empty {@link Optional} is returned.
     * 
     * If FilterOptions is provided, the associated projection will be used.
     * Otherwise, all fields will be returned.
     * 
     * @param uuid
     * @param filterOptions
     * @return
     */
    public Optional<Engagement> findByUuid(String uuid, SingleFilterOptions filterOptions) {
        Bson bson = eq("uuid", uuid);
        return Optional.ofNullable(find(Optional.of(bson), filterOptions).first());
    }

    /**
     * Returns an {@link Optional} containing the {@link Engagement} with the
     * customer and project names. Otherwise, an empty {@link Optional} is returned.
     * 
     * @param customerName
     * @param projectName
     * @return
     */
    public Optional<Engagement> findByCustomerNameAndProjectName(String customerName, String projectName) {
        return findByCustomerNameAndProjectName(customerName, projectName, new SingleFilterOptions());
    }

    /**
     * Returns an {@link Optional} containing the {@link Engagement} with the
     * customer and project names. Otherwise, an empty {@link Optional} is returned.
     * 
     * If FilterOptions is provided, the associated projection will be used.
     * Otherwise, all fields will be returned.
     * 
     * @param customerName
     * @param projectName
     * @param filterOptions
     * @return
     */
    public Optional<Engagement> findByCustomerNameAndProjectName(String customerName, String projectName,
            SingleFilterOptions filterOptions) {
        Bson bson = and(eq("customerName", customerName), eq("projectName", projectName));
        return Optional.ofNullable(find(Optional.of(bson), filterOptions).first());
    }

    /**
     * Returns all {@link Engagement}s.
     * 
     * If FilterOptions is provided, the associated projection will be used.
     * Otherwise, all fields will be returned.
     * 
     * @param filterOptions
     * @return
     */
    public List<Engagement> findAll(SingleFilterOptions filterOptions) {
        return findAll(Optional.empty(), filterOptions);
    }

    /**
     * Returns a {@link List} of {@link Engagement}s. The {@link Bson} filter will
     * be used to query. Otherwise, all {@link Engagement}s will be returned.
     * 
     * If FilterOptions is provided, the associated projection will be used.
     * Otherwise, all fields will be returned.
     * 
     * @param filter
     * @param filterOptions
     * @return
     */
    public List<Engagement> findAll(Optional<Bson> filter, SingleFilterOptions filterOptions) {
        List<Engagement> list = new ArrayList<>();
        find(filter, filterOptions).iterator().forEachRemaining(list::add);
        return list;
    }

    /**
     * Returns a FindIterable with the resulting {@link Bson} filter or all if no
     * filter provided. A projection is added if either the include or exclude is
     * prvided in the FilterOptions.
     * 
     * @param bson
     * @param filterOptions
     * @return
     */
    private FindIterable<Engagement> find(Optional<Bson> bson, SingleFilterOptions filterOptions) {

        Optional<Set<String>> includeSet = filterOptions.getIncludeList();
        Optional<Set<String>> excludeSet = filterOptions.getExcludeList();

        // return only the attributes to include
        if (includeSet.isPresent()) {
            return getFindIterable(bson).projection(include(List.copyOf(includeSet.get())));
        }

        // return only the attributes not excluded
        if (excludeSet.isPresent()) {
            return getFindIterable(bson).projection(exclude(List.copyOf(excludeSet.get())));
        }

        // return full engagement if no filter
        return getFindIterable(bson);

    }

    /**
     * Returns a FindIterable for all {@link Engagement}s or the results of the
     * {@link Bson} filter.
     * 
     * @param bson
     * @return
     */
    private FindIterable<Engagement> getFindIterable(Optional<Bson> bson) {

        if (bson.isPresent()) {
            return mongoCollection().find(bson.get());
        }

        return mongoCollection().find();

    }

}