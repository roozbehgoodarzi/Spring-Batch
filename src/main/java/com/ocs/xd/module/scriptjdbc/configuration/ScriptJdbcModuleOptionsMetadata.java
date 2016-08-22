package com.ocs.xd.module.scriptjdbc.configuration;

/**
 * Created by vakili on 8/13/2016.
 */

import org.springframework.xd.jdbc.ResourcesIntoJdbcJobModuleOptionsMetadata;
import org.springframework.xd.module.options.mixins.BatchJobCommitIntervalOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobDeleteFilesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobRestartableOptionMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
//, BatchJobResourcesOptionMixin.class
@Mixin({ BatchJobDeleteFilesOptionMixin.class, ResourcesIntoJdbcJobModuleOptionsMetadata.class, BatchJobRestartableOptionMixin.class, BatchJobCommitIntervalOptionMixin.class})
public class ScriptJdbcModuleOptionsMetadata {
    private int skipLimit;


    public int getSkipLimit() {
        return skipLimit;
    }

    @ModuleOption("set skipLimit")
    public void setSkipLimit(int skipLimit) {
        this.skipLimit = skipLimit;
    }
/*
    private String resources;

    @ModuleOption("the list of paths to import (Spring resources)")
    public void setResources(String resources) {
        this.resources = resources;
    }

    public String getResources() {
        return this.resources;
    }*/
}
