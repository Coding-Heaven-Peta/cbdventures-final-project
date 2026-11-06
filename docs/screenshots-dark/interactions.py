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
        ctx = await b.new_context(viewport={"width": 390, "height": 844}, device_scale_factor=2)
        pg = await ctx.new_page()
        errs = []
        pg.on("pageerror", lambda e: errs.append(str(e)))
        owner = post("/api/auth/login", {"email": "thabo@kotakingdom.co.za", "password": "Password123!"})
        visitor = post("/api/auth/login", {"email": "keziah@student.richfield.ac.za", "password": "Password123!"})

        # owner: add-listing form
        await pg.goto(f"{BASE}/#/welcome", wait_until="domcontentloaded")
        await pg.evaluate("a=>{localStorage.setItem('cbd.token',a.token);localStorage.setItem('cbd.user',JSON.stringify(a.user));}", owner)
        await pg.goto(f"{BASE}/#/owner", wait_until="domcontentloaded")
        await pg.wait_for_timeout(1800)
        await pg.click("text=Add listing")
        await pg.wait_for_timeout(1200)
        await pg.screenshot(path=f"{OUT}/17-owner-form.png", full_page=True)
        await pg.keyboard.press("Escape")

        # visitor: review modal on a listing
        await pg.goto(f"{BASE}/#/welcome", wait_until="domcontentloaded")
        await pg.evaluate("a=>{localStorage.setItem('cbd.token',a.token);localStorage.setItem('cbd.user',JSON.stringify(a.user));}", visitor)
        await pg.goto(f"{BASE}/#/business/4", wait_until="domcontentloaded")
        await pg.wait_for_timeout(2000)
        try:
            await pg.click("#review-button", timeout=4000)
            await pg.wait_for_timeout(900)
            await pg.screenshot(path=f"{OUT}/18-review-modal.png")
        except Exception as e:
            print("review modal:", e)

        # toast check: clear cache on the profile screen
        await pg.goto(f"{BASE}/#/profile", wait_until="domcontentloaded")
        await pg.wait_for_timeout(1500)
        await pg.click("text=Clear offline cache")
        await pg.wait_for_timeout(500)
        toast = await pg.evaluate("() => document.querySelectorAll('#toast-stack .toast').length")
        print("toasts visible:", toast)
        await pg.screenshot(path=f"{OUT}/19-profile-visitor-toast.png")
        print("PAGE ERRORS:", errs or "none")
        await b.close()

asyncio.run(main())
