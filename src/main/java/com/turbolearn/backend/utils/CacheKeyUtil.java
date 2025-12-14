package com.turbolearn.backend.utils;

import com.turbolearn.backend.tenant.TenantContext;

public class CacheKeyUtil {

    public static String projectListKey(){
        return "TENANT:" + TenantContext.getTenant() + ":PROJECTS";
    }
}
