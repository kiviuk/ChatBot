# Run Spring Boot application with Environment Variable

This guide describes how to run a Spring Boot application with an environment variable passed in, specifically when using the `mvn spring-boot:run` command.

## 1. Run The Service with Environment Variable

- **macOS/Linux:**

   Open a terminal and enter the following command:
    ```bash
    mvn spring-boot:run -Dspring-boot.run.jvmArguments='-DOPENAI_TOKEN=your_openai_token_here'
    ```

## 2. Access Environment Variable in REST Controller

In your REST controller, you can use `System.getProperty("OPENAI_TOKEN");
}` to access the environment variable: