# Caddy vs Nginx на staging

На staging **nginx отдельно не ставится**. Его роль выполняет **Caddy** — это reverse-proxy (как nginx), который:

1. Раздаёт статику фронта из `deploy/frontend/`
2. Проксирует `/api/*` на backend `:8080`
3. **Сам** получает и обновляет TLS-сертификаты Let's Encrypt (аналог **nginx + certbot**)

## Где «конфиг nginx»

| Задача | Nginx (классика) | У нас (Caddy) |
| --- | --- | --- |
| Конфиг reverse-proxy | `/etc/nginx/sites-enabled/...` | [`deploy/Caddyfile`](../Caddyfile) |
| HTTPS-сертификаты | certbot + cron | автоматически в volume `caddy_data` |
| Установка на сервер | `apt install nginx certbot` | образ `caddy:2-alpine` в docker compose |

Фрагмент [`Caddyfile`](../Caddyfile):

```caddy
skinemsya-vse.ru {
    handle /api/* {
        reverse_proxy backend:8080
    }
    handle /actuator/* {
        reverse_proxy backend:8080
    }
    handle {
        root * /srv
        try_files {path} /index.html
        file_server
    }
}
```

Эквивалент в nginx (для понимания, **не используется** на сервере):

```nginx
server {
    listen 443 ssl;
    server_name skinemsya-vse.ru;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
    }
    location / {
        root /var/www/skinemsya;
        try_files $uri /index.html;
    }
}
```

Сертификаты в nginx ставили бы через `certbot --nginx`. Caddy делает это при первом запросе на `:443`, если DNS указывает на сервер и открыт порт 80.

## Если нужен именно nginx

Можно заменить сервис `caddy` на `nginx` + `certbot`, но на слабом VPS это больше ручной работы. Caddy выбран чтобы **не настраивать certbot на хосте**.

## MinIO

MinIO снова в [`docker-compose.prod.yml`](docker-compose.prod.yml): backend хранит чеки в S3-совместимом bucket внутри docker-сети (`http://minio:9000`). Публично MinIO наружу не пробрасывается.
