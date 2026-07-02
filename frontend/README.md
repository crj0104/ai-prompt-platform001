# AI Prompt Template Platform Frontend

This is the separated frontend for the course project.

## Run

1. Create the MySQL database `ai_prompt_template_platform`.

2. Set MySQL connection values with environment variables before starting the backend, for example `DB_USERNAME` and `DB_PASSWORD`.

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
