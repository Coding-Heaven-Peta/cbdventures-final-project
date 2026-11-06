import asyncio, json, time, urllib.request
from playwright.async_api import async_playwright

BASE = "http://localhost:8081"
OUT = "/home/user/verify-dark"


def post(path, payload):
    req = urllib.request.Request(BASE + path, data=json.dumps(payload).encode(),
                                 headers={"Content-Type": "application/json"})
    return json.load(urllib.request.urlopen(req))


def get(path):
    return json.load(urllib.request.urlopen(BASE + path))


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(args=["--no-sandbox"])
        ctx = await browser.new_context(viewport={"width": 390, "height": 844}, device_scale_factor=2)
        page = await ctx.new_page()
        errors = []
        page.on("console", lambda m: errors.append(m.text) if m.type == "error" else None)
        page.on("pageerror", lambda e: errors.append(str(e)))

        async def shot(name, hash_url, wait=1500, full=False):
            await page.goto(f"{BASE}/{hash_url}", wait_until="domcontentloaded")
            await page.wait_for_timeout(wait)
            await page.screenshot(path=f"{OUT}/{name}.png", full_page=full)

        def sign_in(auth):
            return page.evaluate(
                "a => { localStorage.setItem('cbd.token', a.token);"
                "        localStorage.setItem('cbd.user', JSON.stringify(a.user)); }", auth)

        await shot("01-welcome", "#/welcome")
        await shot("02-signin", "#/signin")
        await shot("03-signup", "#/signup")

        stamp = int(time.time())
        email = f"dark.demo.{stamp}@cbdventures.test"
        reg = post("/api/auth/register", {"fullName": "Lerato Mahlangu", "email": email,
                                          "password": "Password123", "role": "VISITOR"})
        await page.goto(f"{BASE}/#/verify?email={email.replace('@', '%40')}", wait_until="domcontentloaded")
        await page.wait_for_timeout(900)
        for i, ch in enumerate(reg.get("demoCode", "")):
            await page.fill(f"#otp input:nth-child({i + 1})", ch)
        await page.wait_for_timeout(300)
        await page.screenshot(path=f"{OUT}/04-verify.png")

        await shot("05-discover-guest", "#/", 2400)
        await shot("06-explore", "#/explore", 2200)
        await page.click("#filter-toggle", timeout=5000)
        await page.wait_for_timeout(700)
        await page.screenshot(path=f"{OUT}/06b-filters.png")

        bid = get("/api/businesses?size=1")["items"][0]["id"]
        await shot("07-business", f"#/business/{bid}", 2200)
        await shot("08-map", "#/map", 2400)
        await shot("09-events", "#/events", 2000)

        # signed-in visitor: the reference "Hello, Explorer!" home
        visitor = post("/api/auth/verify", {"email": email, "code": reg["demoCode"]})
        await page.goto(f"{BASE}/#/welcome", wait_until="domcontentloaded")
        await sign_in(visitor)
        await shot("10-discover-signedin", "#/", 2400)
        await shot("11-profile-visitor", "#/profile", 1800)

        admin = post("/api/auth/login", {"email": "admin@cbdventures.co.za", "password": "Admin123!"})
        await page.goto(f"{BASE}/#/welcome", wait_until="domcontentloaded")
        await sign_in(admin)
        await shot("12-profile-admin", "#/profile", 1800)
        await shot("13-admin", "#/admin", 2200)
        await shot("14-reports", "#/reports", 2400)
        await shot("15-backup", "#/backup", 2000)

        owner = post("/api/auth/login", {"email": "thabo@kotakingdom.co.za", "password": "Password123!"})
        await page.goto(f"{BASE}/#/welcome", wait_until="domcontentloaded")
        await sign_in(owner)
        await shot("16-owner", "#/owner", 2200)

        print("CONSOLE ERRORS:", errors[:8] if errors else "none")
        await browser.close()

asyncio.run(main())
