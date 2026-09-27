# Finance Tracker Frontend

React TypeScript frontend for the existing Spring Boot API.

## Run

```bash
npm install
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8000`. To point at another backend URL, create `.env`:

```bash
VITE_API_BASE_URL=http://localhost:8000
VITE_API_PROXY_TARGET=http://host.docker.internal:8000
```

When running inside Docker, publish the Vite port and bind Vite to all interfaces:

```bash
docker run --rm -it -p 5173:5173 -v "%cd%":/app -w /app/frontend node:26-slim npm run dev
```

## API Coverage

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/test`
- `GET /api/bank/list/banks`
- `GET /api/bank/banks/accounts`
- `POST /api/bank/create/bank`
- `POST /api/bank/create/account`
- `GET /api/txn/list`
- `POST /api/txn/create`
- `POST /api/txn/create/share`
