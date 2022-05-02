# Compsosum Nodes Debugutils

This bundle contains various utilities that can help debugging and inspecting things during development on Composum or
AEM. It is independent from Composum - it can be deployed on a bare Sling Starter, on servers carrying Composum or on
AEM.

This is done in a way that it has a low risk when being deployed on development / QA systems: all components here (
mostly servlets and servlet filters) are done so that you have to explicitly enable them in the OSGI configuration
before they become active (ConfigurationPolicy.REQUIRE). If it's a servlet that is not only relevant for the author server,
there has to be a resource with the corresponding sling:resourceType present, so that they can be rendered. 
We do not bind the servlets to a path since that isn't always available, as in the case of AEM as a cloud service (AEMaaCS)
and paths like /bin/ are also often blocked by an apache or dispatcher that is put in front of the server. 
Also, we usually use .html as extension and react to GET requets to circumvent such restrictions.

(In some cases, such as AEM as a cloud service, there is no browser on the publish server, so it isn't easily possible
to create such a resource on publish. In that case, such a resource can be created with the browser as subresource on
a page, and this page can be published so that it's present on publish.)

DISCLAIMER: Since this is just for debugging, please consider the components of alpha quality - use at your own risk!
