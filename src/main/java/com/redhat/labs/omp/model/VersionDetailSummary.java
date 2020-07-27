package com.redhat.labs.omp.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionDetailSummary {

    private VersionDetail mainVersion;
    private List<VersionDetail> componentVersions;

}
