package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.wso2.am.integration.cucumbertests.TestContext;
import org.wso2.am.integration.cucumbertests.UnzipUtil;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.ModulePathResolver;
import org.wso2.am.testcontainers.*;
import io.cucumber.datatable.DataTable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import java.io.IOException;

public class ContainorIntitialisationStepDefinitions {
    String baseUrl;
    String serviceBaseUrl;
    String baseGatewayUrl;
    Integer HTTPS_PORT=8243;
    Integer HTTP_PORT=8280;
    CustomAPIMContainer customApimContainer;
    String callerModuleDir = ModulePathResolver.getModuleDir(ContainorIntitialisationStepDefinitions .class);

    private final TestContext context;

    public ContainorIntitialisationStepDefinitions(TestContext context) {
        this.context = context;
    }

    @Given("I have initialized the Default API Manager container")
    public void initializeDefaultAPIMContainer() {
        DefaultAPIMContainer apimContainer = DefaultAPIMContainer.getInstance();
        baseUrl = apimContainer.getAPIManagerUrl();
        context.set("baseUrl",baseUrl);
        Integer gatewayPort= apimContainer.getMappedPort(HTTPS_PORT);
        String gatewayHost = apimContainer.getHost();
        baseGatewayUrl= String.format("https://%s:%d", gatewayHost, gatewayPort);
        context.set("baseGatewayUrl",baseGatewayUrl);
        context.set("label","default");
    }

    @Given("I have initialized the Custom API Manager container with label {string} and deployment toml file path at {string}")
    public void initializeCustomAPIMContainer(String label,String tomlPath) throws IOException, InterruptedException {
        String fullPath = callerModuleDir+tomlPath;
        customApimContainer = new CustomAPIMContainer(label,fullPath);
        customApimContainer.start();

        // Verifying that the file was copied correctly
        String filePathInsideContainer = "/opt/repository/conf/deployment.toml";
        String fileContents = customApimContainer.execInContainer("cat", filePathInsideContainer).getStdout();
        System.out.println("Contents of the copied deployment.toml inside the container:");
        System.out.println(fileContents);

        baseUrl = customApimContainer.getAPIManagerUrl();
        context.set("baseUrl",baseUrl);
        Integer gatewayPort= customApimContainer.getMappedPort(HTTPS_PORT);
        String gatewayHost = customApimContainer.getHost();
        baseGatewayUrl= String.format("https://%s:%d", gatewayHost, gatewayPort);
        context.set("baseGatewayUrl",baseGatewayUrl);
        context.set("label",label);
    }

    @Then("I stop the Custom API Manager container")
    public void endCustomAPIMContainer(){
//       customApimContainer.stop();
       customApimContainer.close();
    }

    @Given("I have initialized the Tomcat server container")
    public void initializeTomcatServerContainer() {

        TomcatServer.getInstance();
        serviceBaseUrl = "http://tomcatbackend:8080/";
        context.set("serviceBaseUrl",serviceBaseUrl);
    }

    @Given("I have initialized the NodeApp server container")
    public void initializeNodeAppServerContainer() {
        NodeAppServer.getInstance();
    }

    @Given("I have initialized test instance with the following configuration")
    public void initializeAPIMContainerWithDataTable(DataTable dataTable) {
        Map<String, String> config = dataTable.asMap(String.class, String.class);

        String thisBaseUrl = config.getOrDefault("baseUrl", baseUrl);
        context.set("baseUrl", thisBaseUrl);
        String thisBaseGatewayUrl = config.getOrDefault("baseGatewayUrl", baseGatewayUrl);
        context.set("baseGatewayUrl", thisBaseGatewayUrl);
        String thisServiceBaseUrl = config.getOrDefault("serviceBaseUrl", serviceBaseUrl);
        context.set("serviceBaseUrl", thisServiceBaseUrl);
        String label = config.getOrDefault("label", "local");
        context.set("label", label);
    }

        @Given("The zip file at relative location {string} is extracted to {string}")
        public void the_zip_file_is_extracted_to(String zipRelativePath, String extractRelativePath) throws IOException {
//            String zipPath = projectRoot + File.separator + zipRelativePath;

            String callerModuleDestDir = ModulePathResolver.getModuleDir(BaseAPIMContainer .class);
            String extractTo = callerModuleDestDir  + extractRelativePath;
            String zipDir = context.get("repoUrl").toString() + zipRelativePath;

            // Unzip using the utility
            try {
//            UnzipUtil.unzip(zipPath, extractTo);
                UnzipUtil.unzip(zipDir, extractTo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    @Given("The repository directory path is {string}")
    public void setRepositoryDirectoryPath(String repoPath) {
               // Save to test context (assuming TestContext is your context manager)
        context.set("repoUrl",repoPath);
    }

    @Given("The repository directory path is set to the test context")
    public void setRepositoryDirectoryPath() {

       Path repoRoot = Paths.get(System.getProperty("module.dir")).toAbsolutePath().normalize();

        while (!repoRoot.endsWith(Constants.REPOSITORY_ROOT) && repoRoot.getParent() != null) {
            repoRoot = repoRoot.getParent();
        }
        if (!repoRoot.endsWith(Constants.REPOSITORY_ROOT)) {
            throw new RuntimeException("Repository root " + Constants.REPOSITORY_ROOT + " not found in path hierarchy");
        }
        context.set("repoUrl",repoRoot);
    }

    @Then("I clear the context")
    public void clearContext(){
        context.clear();
    }

}


