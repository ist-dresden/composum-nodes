# Compsosum Nodes Debugutils

This bundle contains various utilities that can help debugging and inspecting things during development on Composum or
AEM. It is independent from Composum - it can be deployed on a bare Sling Starter, on servers carrying Composum or on
AEM.

This is done in a way that it has a low risk when being deployed on development / QA systems: all components here (
mostly servlets and servlet filters) are done so that you have to explicitly enable them in the OSGI configuration
before they become active (ConfigurationPolicy.REQUIRE).

Since this is just for debugging, please consider the components of alpha quality.

