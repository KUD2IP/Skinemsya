# Nginx на staging

**nginx** установлен на хосте вручную. Конфиг **не хранится в репозитории** — создаётся на сервере в `/etc/nginx/sites-available/skinemsya`.

Docker Compose: postgres, minio, backend (`127.0.0.1:8080`).

## Схема

```
:443 nginx → /opt/skinemsya/deploy/frontend/  (фронт)
         → 127.0.0.1:8080                     (/api/, /actuator/)
```

См. [`docs/deployment/staging-server.md`](../staging-server.md) — раздел 2.7.
