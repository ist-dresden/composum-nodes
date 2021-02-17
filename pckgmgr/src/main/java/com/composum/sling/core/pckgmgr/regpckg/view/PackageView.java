package com.composum.sling.core.pckgmgr.regpckg.view;

import com.composum.sling.core.BeanContext;
import org.apache.jackrabbit.vault.packaging.PackageId;

import java.io.IOException;

public interface PackageView {

    String getNamespace();

    PackageId getPackageId();

    String getGroup();

    String getName();

    String getVersion();

    boolean isValid();

    boolean isInstalled();

    boolean isClosed();

    boolean isLoaded();

    void load(BeanContext context) throws IOException;
}
