# Staging на VPS (skinemsya-vse.ru)

Пошаговая инструкция для **пустого Ubuntu 24.04** и деплоя через **GitHub Actions**.  
Сервер слабый (~1 GB RAM, ~10 GB диск) — стек минимальный: **PostgreSQL + MinIO + backend + Caddy** (HTTPS и раздача фронта).

---

## Что получится в итоге

| URL | Назначение |
| --- | --- |
| `https://skinemsya-vse.ru` | Mini App (статика) |
| `https://skinemsya-vse.ru/api/v1/...` | Backend API (прокси Caddy → backend) |
| `https://skinemsya-vse.ru/actuator/health` | Healthcheck backend |

Фронт ходит на API **с того же origin** (`/api/v1`), отдельный поддомен для API не нужен.

---

## Часть 1. DNS

У регистратора домена `skinemsya-vse.ru` создай A-записи на **публичный IPv4** VPS:

| Имя | Тип | Значение |
| --- | --- | --- |
| `@` | A | `<публичный IP сервера>` |
| `www` | A | `<публичный IP сервера>` |

Подожди 5–30 минут, проверь:

```bash
dig +short skinemsya-vse.ru
dig +short www.skinemsya-vse.ru
```

Оба должны вернуть IP сервера.

---

## Часть 2. Подготовка сервера (один раз)

Подключись по SSH под `root` (или пользователем с sudo).

### 2.1. Обновление и базовые пакеты

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y ca-certificates curl gnupg ufw
```

### 2.2. Пользователь для деплоя

```bash
sudo adduser --disabled-password --gecos "" deploy
```

После установки Docker (шаг 2.3):

```bash
sudo usermod -aG docker deploy
```

### 2.3. Docker Engine + Compose plugin

```bash
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
| sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker deploy
```

Проверка:

```bash
docker --version
docker compose version
```

### 2.4. Firewall

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

### 2.5. Swap (рекомендуется для 1 GB RAM)

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 2.6. Каталог приложения

```bash
sudo mkdir -p /opt/skinemsya/deploy/frontend
sudo chown -R deploy:deploy /opt/skinemsya
```

### 2.7. SSH-ключ для CI/CD

**На своей машине** (не на сервере):

```bash
ssh-keygen -t ed25519 -C "github-deploy" -f ~/.ssh/skinemsya_deploy -N ""
```

Публичный ключ добавь на сервер:

```bash
sudo mkdir -p /home/deploy/.ssh
sudo nano /home/deploy/.ssh/authorized_keys
# вставь содержимое ~/.ssh/skinemsya_deploy.pub

sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

Проверка входа:

```bash
ssh -i ~/.ssh/skinemsya_deploy deploy@<IP_СЕРВЕРА>
```

### 2.8. Секреты приложения (.env)

**На сервере `.env` вручную не создаётся** — его кладёт job `deploy-staging` из GitHub Actions (см. часть 4).

Локально подготовь файл по шаблону `deploy/env.production.example` и добавь в GitHub Secrets.

---

## Часть 3. Telegram BotFather

1. В @BotFather → **Bot Settings → Domain** → добавь `skinemsya-vse.ru`.
2. **Menu Button / Web App** → URL: `https://skinemsya-vse.ru`.
3. Если используешь short name (`t.me/bot/app`) — пропиши `TELEGRAM_WEB_APP_SHORT_NAME` в `.env`.

---

## Часть 4. GitHub Actions secrets

Репозиторий backend → **Settings → Secrets and variables → Actions → New repository secret**.

### 4.1. Секреты приложения (один из вариантов)

**Вариант A (рекомендуется):** secret `STAGING_ENV` — вставь **весь** `.env` целиком (GitHub поддерживает multiline secrets).

```bash
cp deploy/env.production.example ~/skinemsya-staging.env
nano ~/skinemsya-staging.env
# скопируй содержимое в secret STAGING_ENV
```

**Вариант B:** secret `STAGING_ENV_B64` — base64 всего файла:

```bash
base64 < ~/skinemsya-staging.env | pbcopy   # macOS
# или: base64 -w0 ~/skinemsya-staging.env   # Linux
```

Pipeline допишет `APP_VERSION=<commit>` при деплое.

### 4.2. Переменные для SSH

| Secret | Обязательный | Назначение |
| --- | --- | --- |
| `SSH_PRIVATE_KEY` | ✅ | приватный ключ `skinemsya_deploy` (весь файл, включая `-----BEGIN...`) |
| `STAGING_HOST` | ✅ | IP или домен VPS (`skinemsya-vse.ru`) |
| `STAGING_USER` | ✅ | `deploy` |
| `STAGING_SSH_PORT` | ❌ | `22` (если нестандартный порт) |
| `SSH_KNOWN_HOSTS` | ❌ | `ssh-keyscan -p 22 skinemsya-vse.ru` (если не задан — workflow сгенерирует сам) |

**Как правильно добавить `SSH_PRIVATE_KEY`:**

```bash
# На своей машине — скопируй ВЕСЬ приватный ключ (не .pub!)
cat ~/.ssh/skinemsya_deploy
```

В GitHub secret вставь **целиком**, включая строки:
```
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

Проверка локально (должен войти без пароля):

```bash
ssh -i ~/.ssh/skinemsya_deploy deploy@skinemsya-vse.ru
```

На сервере публичный ключ должен быть в `/home/deploy/.ssh/authorized_keys`.

Получить `SSH_KNOWN_HOSTS` (опционально):

```bash
ssh-keyscan -p 22 skinemsya-vse.ru
```

### 4.3. Environment `staging` (опционально)

В **Settings → Environments** создай environment `staging` и включи **Required reviewers**, если нужно подтверждение деплоя перед запуском job.

### 4.4. Репозиторий фронта (`skinemsya_ui`)

Фронт деплоится **отдельным workflow** из репозитория UI. Задай там те же SSH-secrets (`SSH_PRIVATE_KEY`, `STAGING_HOST`, `STAGING_USER`, `STAGING_SSH_PORT`).

`STAGING_ENV` фронту **не нужен** — секреты только у backend.

Полная инструкция по фронту: репозиторий `skinemsya_ui` → `docs/DEPLOYMENT.md`.

Фронт собирается без `VITE_API_BASE_URL`: запросы идут на `/api/v1` того же домена.

Job деплоя фронта кладёт `dist/` в `/opt/skinemsya/deploy/frontend/` — Caddy сразу отдаёт новые файлы, перезапуск не нужен.

---

## Часть 5. Первый деплой

### 5.1. Backend (этот репозиторий)

1. Push в **main** (или **master**).
2. Дождись успешного workflow **Backend CI/CD** (jobs `Unit tests` + `Build Docker image`).
3. **Actions → Backend CI/CD → Run workflow**:
   - ветка: `main`
   - ✅ **Deploy to staging after build**
4. Дождись job `Deploy to staging`.

### 5.2. Frontend (репозиторий `skinemsya_ui`)

1. Push в **main**.
2. Дождись `Typecheck` + `Build`.
3. **Actions → Frontend CI/CD → Run workflow** → ✅ **Deploy to staging**.

Инструкция: `skinemsya_ui/docs/DEPLOYMENT.md`.

Порядок первого раза: **сначала backend** (поднимет Postgres, MinIO, Caddy), **потом frontend** (положит статику в `deploy/frontend/`).

### 5.3. Проверка

```bash
cd /opt/skinemsya/deploy
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
curl -fsS https://skinemsya-vse.ru/actuator/health
```

Открой бота в Telegram → Mini App.

---

## Часть 6. Обновления

| Репозиторий | Автоматически (push в main) | Вручную |
| --- | --- | --- |
| backend | unit-тесты, сборка Docker-образа | Run workflow → Deploy to staging |
| frontend | typecheck, build | Run workflow → deploy |

Backend и frontend деплоятся **независимо**.

### Быстрый деплой backend после push

1. Push в `main` → дождись зелёной сборки.
2. **Actions → Backend CI/CD → Run workflow** → включи **Deploy to staging**.

---

## Ручной деплой (если CI недоступен)

На машине с исходниками:

```bash
docker build -t skinemsya-backend:manual .
docker save skinemsya-backend:manual | gzip > skinemsya-backend.tar.gz
scp skinemsya-backend.tar.gz deploy/ deploy@<IP>:/opt/skinemsya/

# Frontend (в каталоге skinemsya_ui)
npm ci && npm run build
rsync -avz --delete dist/ deploy@<IP>:/opt/skinemsya/deploy/frontend/
```

На сервере:

```bash
cd /opt/skinemsya
gunzip -c skinemsya-backend.tar.gz | docker load
cd deploy
cp /path/to/your/.env .env   # или создай из deploy/env.production.example
chmod 600 .env
APP_VERSION=manual docker compose -f docker-compose.prod.yml up -d
```

---

## Диагностика

| Симптом | Что проверить |
| --- | --- |
| 502 / API недоступен | `docker compose logs backend` |
| Нет HTTPS | DNS, порты 80/443, `docker compose logs caddy` |
| Mini App не авторизует | `TELEGRAM_BOT_TOKEN`, домен в BotFather |
| OOM | `free -h`, swap, `docker stats` |
| Deploy job падает на SSH | `SSH_PRIVATE_KEY`, `STAGING_HOST`, `authorized_keys` на сервере |
| `Permission denied (publickey)` | См. чеклист ниже |
| Deploy job падает на .env | secret `STAGING_ENV` или `STAGING_ENV_B64` |

### `Permission denied (publickey)` — чеклист

1. **GitHub secret `SSH_PRIVATE_KEY`** — это **приватный** ключ (`skinemsya_deploy`), не `.pub`
2. Ключ вставлен **целиком** с `BEGIN`/`END` строками
3. На сервере в `/home/deploy/.ssh/authorized_keys` лежит **соответствующий публичный** ключ
4. `STAGING_USER=deploy`, `STAGING_HOST` — IP или домен сервера
5. Локально работает: `ssh -i ~/.ssh/skinemsya_deploy deploy@<HOST>`
6. Secrets заданы в **том репозитории**, из которого запускаешь deploy (backend и frontend — отдельно)

---

## Ограничения staging

- ~512 MB backend + ~256 MB Postgres — только для проверки.
- Файлы в MinIO (S3-совместимое хранилище в docker).
- ML-сервис не развёрнут без отдельной настройки `ML_SERVICE_URL`.
