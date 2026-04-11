# AutoTxt

Spring Boot app that:

- Shows ChinaNews "scroll-news" RSS headlines by date/keyword
- Lets users subscribe an email address
- Sends yesterday's Top 10 headlines every day at 09:00 (Asia/Shanghai)

## Run locally

```bash
cd autotxt
mvn spring-boot:run
```

Open:

- `http://localhost:8080/` (headlines)
- `http://localhost:8080/subscribe` (email subscription + "send yesterday top10" test button)

## Mail configuration (recommended via env vars)

Set SMTP and AutoTxt env vars (examples):

```bash
export AUTOTXT_MAIL_ENABLED=true
export AUTOTXT_MAIL_FROM="you@qq.com"

export SPRING_MAIL_HOST="smtp.qq.com"
export SPRING_MAIL_PORT="465"
export SPRING_MAIL_USERNAME="you@qq.com"
export SPRING_MAIL_PASSWORD="YOUR_APP_PASSWORD"
export SPRING_MAIL_SMTP_SSL_ENABLE="true"
export SPRING_MAIL_SMTP_SSL_TRUST="smtp.qq.com"
```

Subscribers are stored at `data/subscribers.txt` by default (ignored by git).

