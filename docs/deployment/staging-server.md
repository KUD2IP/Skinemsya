# Staging на VPS (skinemsya-vse.ru)

Пошаговая инструкция для **пустого Ubuntu 24.04** и деплоя через **GitFlic CI/CD**.  
Сервер слабый (~1 GB RAM, ~10 GB диск) — стек минимальный: **PostgreSQL + backend + Caddy** (HTTPS и раздача фронта). MinIO и ML-сервис на staging не поднимаем.

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
ssh-keygen -t ed25519 -C "gitflic-deploy" -f ~/.ssh/skinemsya_deploy -N ""
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

**На сервере `.env` вручную не создаётся** — его кладёт job `deploy:staging` из GitFlic (см. часть 4).

Локально подготовь файл по шаблону `deploy/env.production.example` и загрузи в GitFlic одной переменной `STAGING_ENV_FILE`.

---

## Часть 3. Telegram BotFather

1. В @BotFather → **Bot Settings → Domain** → добавь `skinemsya-vse.ru`.
2. **Menu Button / Web App** → URL: `https://skinemsya-vse.ru`.
3. Если используешь short name (`t.me/bot/app`) — пропиши `TELEGRAM_WEB_APP_SHORT_NAME` в `.env`.

---

## Часть 4. GitFlic CI/CD variables

### 4.1. Один файл с секретами приложения

1. Скопируй шаблон локально:
   ```bash
   cp deploy/env.production.example ~/skinemsya-staging.env
   nano ~/skinemsya-staging.env
   ```
2. В GitFlic → **Settings → CI/CD → Variables** → **Добавить**:
   - **Ключ:** `STAGING_ENV_FILE`
   - **Тип:** **Файл** (File)
   - **Значение:** вставь содержимое `skinemsya-staging.env` (или загрузи файл)
   - **Masked:** по желанию (для файла обычно не нужно)

Pipeline скопирует его в `deploy/.env` на сервере и допишет `APP_VERSION=<commit>`.

**Альтернатива**, если тип «Файл» недоступен: одна переменная `STAGING_ENV_B64` — base64 всего `.env`:
```bash
base64 < ~/skinemsya-staging.env | pbcopy   # macOS
```

### 4.2. Переменные для SSH (репозиторий backend)

| Variable | Masked | Назначение |
| --- | --- | --- |
| `SSH_PRIVATE_KEY` | ✅ | приватный ключ `skinemsya_deploy` |
| `SSH_KNOWN_HOSTS` | ❌ | `ssh-keyscan -p 22 skinemsya-vse.ru` |
| `STAGING_HOST` | ❌ | IP или домен VPS |
| `STAGING_USER` | ❌ | `deploy` |
| `STAGING_SSH_PORT` | ❌ | `22` |

### 4.3. Репозиторий фронта (`skinemsya_ui`)

Фронт деплоится **отдельным pipeline** из репозитория `skinemsya_ui` (файл `gitflic-ci.yaml` там же).

В **репозитории UI** задай те же SSH-переменные, что в п. 4.2 (`SSH_PRIVATE_KEY`, `SSH_KNOWN_HOSTS`, `STAGING_*`).

`STAGING_ENV_FILE` фронту **не нужен** — секреты только у backend.

Фронт собирается без `VITE_API_BASE_URL`: запросы идут на `/api/v1` того же домена (`https://skinemsya-vse.ru`).

Job **`deploy:staging`** в UI-каталоге кладёт `dist/` в `/opt/skinemsya/deploy/frontend/` — Caddy сразу отдаёт новые файлы, перезапуск не нужен.

Получить `SSH_KNOWN_HOSTS`:

```bash
ssh-keyscan -p 22 skinemsya-vse.ru
```

Скопируй **весь** вывод в переменную.

### Runner

Нужен GitFlic Runner с поддержкой `services: docker:dind` для job `build:backend-image`.

- `test:unit` — обычный Maven-контейнер.
- `build:backend-image` — Docker-in-Docker.
- `deploy:staging` — SSH на VPS (ручной запуск).

---

## Часть 5. Первый деплой

### 5.1. Backend (репозиторий `skinemsya_java`)

1. Push в **main/master**.
2. Дождись `test:unit` и `build:backend-image`.
3. Запусти вручную **`deploy:staging`**.

### 5.2. Frontend (репозиторий `skinemsya_ui`)

1. Push в **main/master**.
2. Дождись `test:typecheck` и `build`.
3. Запусти вручную **`deploy:staging`**.

Порядок первого раза: **сначала backend** (поднимет Postgres, MinIO, Caddy), **потом frontend** (положит статику в `deploy/frontend/`).

### 5.3. Проверка

```bash
cd /opt/skinemsya/deploy
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
curl -fsS https://skinemsya-vse.ru/actuator/health
```

5. Открой бота в Telegram → Mini App.

---

## Часть 6. Обновления

| Репозиторий | Автоматически | Вручную |
| --- | --- | --- |
| `skinemsya_java` | unit-тесты, сборка Docker-образа | `deploy:staging` |
| `skinemsya_ui` | typecheck, `npm run build` | `deploy:staging` |

Backend и frontend деплоятся **независимо** — можно обновить только API или только UI.

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

---

## Ограничения staging

- ~512 MB backend + ~256 MB Postgres — только для проверки.
- Файлы локально в volume, не S3.
- ML-сервис не развёрнут без отдельной настройки `ML_SERVICE_URL`.
