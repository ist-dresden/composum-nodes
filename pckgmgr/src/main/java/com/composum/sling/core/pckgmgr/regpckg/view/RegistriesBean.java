package com.composum.sling.core.pckgmgr.regpckg.view;

import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries;
import com.composum.sling.core.pckgmgr.regpckg.service.PackageRegistries.Registries;
import com.composum.sling.nodes.console.ConsoleSlingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RegistriesBean extends ConsoleSlingBean {

    private static final Logger LOG = LoggerFactory.getLogger(RegistriesBean.class);

    private transient Registries registries;

    public Registries getRegistries() {
        if (registries == null) {
            PackageRegistries service = context.getService(PackageRegistries.class);
            registries = service.getRegistries(context.getResolver());
        }
        return registries;
    }

    public PackageBean getPackage(String path) throws IOException {
        return new PackageBean(context, path);
    }
}
