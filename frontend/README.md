# AI Prompt Template Platform Frontend

This is the separated frontend for the course project.

## Run

1. Create the MySQL database `ai_prompt_template_platform`.

2. Fill in MySQL username and password in `../backend/src/main/resources/application.properties`.

3. Start the backend in `../backend`:

   ```bash
   mvn spring-boot:run
   ```

4. Open `frontend/index.html` in a browser.

The default API base is `http://localhost:8080/api`.
If the backend uses another port, open:

```text
frontend/index.html?apiBase=http://localhost:8081/api
```
