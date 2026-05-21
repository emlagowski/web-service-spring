# SOAP XML Signature Blog

Static HTML article for the Spring Boot SOAP XML Signature demo.

## Local Preview

The page has no build step. Run a small local server from this directory so the page can also load bundled log files from `logs/client.log` and `logs/provider.log`:

```bash
cd webservice-blog
python3 -m http.server 4173
```

Open:

```text
http://localhost:4173
```

Opening `index.html` directly also renders the article, but browser security rules can block loading the external log files from disk.

## Vercel

Use `webservice-blog/` as the Vercel project root.

Suggested settings:

- framework preset: `Other`
- build command: empty
- output directory: empty or `.`
- target domain: `https://webservice.mlagowski.com`

## Links

- Demo repository: https://github.com/emlagowski/web-service-spring
- Author website: https://www.mlagowski.com
- Article home: https://webservice.mlagowski.com
