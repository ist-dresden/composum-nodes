# Composum Nodes as Packages

This directory contains composum Nodes as package instead of bundles:
- aem/ contains a package for installation on AEM. For this, we exclude the package manager, since AEM has it's own package manager.
- sling/ contains a package for installation on Sling. This requires a package manager, for instance the Composum package manager contained in the sling starter. It is able to update an older composum Core (version 1.x) installation, even when replacing the Composum Package Manager from within itself.

There is a setup/ folder containing a setup bundle, that aids in transitioning from Composum Core version 1.x to Composum Nodes (including removal of the old bundles), and a setup hook (adapting the changed clientlib render directory in this transition). 

Furthermore, there are bundles (such as commons, console, ...) specifically for the inclusion into these packages which are built from the normal composum bundles that filter the initial content from the normal bundles. The initial content is put instead into the content in the sling/ bundle, which seems to provide for a more reliable updating of the content.

The jslibs/ package is included into the sling/ package and is installed directly from there. As for AEM, it's content is embedded into the aem/ package directly.

## Implementation note

Since the composum bundles are also meant for installation into the Sling Starter directly, without any package manager, it is currently not possible to move the initial content into a package. Thus, we go for the approach to uild alternate versions of these bundles here that strip them from the initial content, and also build a package from their initial content.
