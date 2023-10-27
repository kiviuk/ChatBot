# Run Spring Boot application with Environment Variable

This guide describes how to run a Spring Boot application with an environment variable passed in, specifically when using the `mvn spring-boot:run` command.

## 1. Executing the Service with OpenAI Key as a System Variable

- **macOS/Linux:**

   Open a terminal and enter the following command:
    ```bash
    mvn spring-boot:run -Dspring-boot.run.jvmArguments='-DOPENAI_API_KEY=your_openai_token_here'
    ```

## 2. Retrieving the OpenAI Key Variable within REST Controller

In your REST controller, you can use `System.getProperty("OPENAI_API_KEY");
}` to access the environment variable:

# Further Reading

- [How to stream Azure OpenAI from Spring API to WebClient](https://ondrej-kvasnovsky.medium.com/how-to-stream-azure-openai-from-spring-api-to-web-client-74eb61db59fc)
- [Azure OpenAI README on MS Docs](https://learn.microsoft.com/en-us/java/api/overview/azure/ai-openai-readme?view=azure-java-preview)