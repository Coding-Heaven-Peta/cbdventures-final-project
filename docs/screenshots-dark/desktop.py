import asyncio, json, urllib.request
from playwright.async_api import async_playwright

BASE = "http://localhost:8081"
OUT = "/home/user/verify-dark"

def post(path, payload):
    req = urllib.request.Request(BASE + path, data=json.dumps(payload).encode(),
                                 headers={"Content-Type": "application/json"})
    return json.load(urllib.request.urlopen(req))

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--no-sandbox"])
        ctx = await b.new_context(viewport={"width": 1280, "height": 900}, device_scale_factor=1)
        pg = await ctx.new_page()
        await pg.goto(f"{BASE}/#/", wait_until="domcontentloaded")
        await pg.wait_for_timeout(2400)
        await pg.screenshot(path=f"{OUT}/20-desktop-discover.png")
        admin = post("/api/auth/login", {"email": "admin@cbdventures.co.za", "password": "Admin123!"})
        await pg.evaluate("a=>{localStorage.setItem('cbd.token',a.token);localStorage.setItem('cbd.user',JSON.stringify(a.user));}", admin)
        await pg.goto(f"{BASE}/#/reports", wait_until="domcontentloaded")
        await pg.wait_for_timeout(2200)
        await pg.screenshot(path=f"{OUT}/21-desktop-reports.png")
        await b.close()

asyncio.run(main())
