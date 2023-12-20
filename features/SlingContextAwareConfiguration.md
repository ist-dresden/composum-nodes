# Editor for Sling Context-Aware Configuration

## Links

https://sling.apache.org/documentation/bundles/context-aware-configuration/context-aware-configuration-default-implementation.html

## Background

In the Composum Browser, it's possible to select a resource in the JCR resource tree on the left. On the right, there
are various view tabs that can be selected. One of them will be the CAConfig view. A view tab typically has an action
bar on top with several buttons, and then the view content. If actions in the action bar apply to one or more items from
the view content, the view content typically has radio buttons or checkboxes to select the items the action applies to,
and then the action bar buttons are enabled and apply to the selected items. For operations that need parameters a
dialog is opened. Goal of this specification is to define the new views and the actions in the action bar for the
CAConfig view depending on the resource type, and the dialogs that are opened for the operations if necessary.

## Basic idea for the feature

We want to integrate viewing and editing of Sling Context-Aware Configuration (CAConfiguration) into the Composum Nodes
Browser.
One tension that plays a role here is that the browser is focused on editing a specific JCR resource, while the
CAConfiguration resolution is dependent on the resource and it's parent resources and the tree of the actual
configurations at /conf that is referred from the resource and / or it's parents.
Also, the editor should use the metadata from the `ConfigurationManager` to provide a good editing experience, and
perhaps validation.
The editor should represent and allow the user to edit the inheritance done through the attributes  
sling:configCollectionInherit or sling:configPropertyInherit.

The editor is implemented as an additional view "CAConfig" for the resource in the browser, which has different modi.
There are the following types of resources, which have different views:

1. resources outside /conf, the configuration fallbacks /apps/conf and /libs/conf and /content: the view should not be
   shown at all.
2. resources in /content: this is a readonly view what the configuration of the resource is, as taken from the hierarchy
   in /conf observing inheritance etc. Editing is only possible on the configuration resources in /conf.

For resources in /conf//sling:configs, /apps/conf//sling:configs and /libs/conf//sling:configs :

3. sling:configs node
4. sling:configs/x.y.z.config for collections
5. sling:configs/x.y.z.config node for non-collections
6. sling:configs/x.y.z.config/subnode for collections

## Supported operations

The editor should support the following operations:

- create / update / delete a configuration
- create / update / delete a configuration collection
- go from a resource in /content to the configuration resource in /conf . The hierarchy of offered configuration
  resources is determined by the `ConfigurationManager` service.
- (possibly) create a new configuration resource in /conf that is more specific than currently existing ones, and
  possibly also create the parent resources in /conf that are needed for this. (Likely that needs an assumed pattern:
  /conf/{path} for /content/{path}, and we add a sling:configRef attribute.);
- setting sling:configPropertyInherit / sling:configCollectionInherit attributes for configurations.

## Basic implementation decisions

For resources in /content the configuration editor is read only - there is no button to add/delete/update a
configuration. It just offers to go to the relevant /conf resource, and there you have the buttons to add/delete/update.

## User stories



PROMPT: print a possible continuation of this specfication from here.
