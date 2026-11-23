from playwright.sync_api import sync_playwright
from pathlib import Path
import json, re, os
root=Path(__file__).resolve().parents[1]
p=root/'src/main/resources/static'
out=root/'docs/screenshots';out.mkdir(parents=True,exist_ok=True)

customer_mock=r'''<script>
window.testRequests=[];
window.fetch=async (url,init={})=>{
 const body=init.body?JSON.parse(init.body):{};window.testRequests.push({url:String(url),body,headers:init.headers||{}});
 let data, status=200;
 if(String(url).startsWith('/api/availability')) data={slots:['09:00','09:30','10:00','10:30','11:00','11:30']};
 else if(url==='/api/verify/send') data={sent:true};
 else if(url==='/api/appointments') {status=201;data={id:1,date:body.date,time:body.time,phone:body.phone};}
 else if(url==='/api/appointments/mine') data={appointments:[{id:99,date:body.date||'2026-12-01',time:'10:00'}],notifications:[]};
 else data={ok:true};
 return new Response(JSON.stringify(data),{status,headers:{'Content-Type':'application/json'}});
}
</script>'''
admin_mock=r'''<script>
window.testRequests=[];
window.fetch=async (url,init={})=>{
 const body=init.body?JSON.parse(init.body):{};window.testRequests.push({url:String(url),body,headers:init.headers||{}});
 let data={ok:true};
 if(url==='/api/admin/session')data={loggedIn:true,username:'admin',csrfToken:'demo-csrf'};
 else if(String(url).startsWith('/api/admin/appointments?'))data=[{id:1,time:'10:00',name:'דנה לוי',phone:'0501234567'},{id:2,time:'11:00',name:'יוסי כהן',phone:'0507654321'}];
 else if(String(url).startsWith('/api/admin/blocks?'))data=[{id:5,label:'14:00–15:00',reason:'הפסקה'}];
 else if(url==='/api/admin/settings')data={slotMinutes:30,days:['ראשון','שני','שלישי','רביעי','חמישי','שישי','שבת'].map((dayName,dayOfWeek)=>({dayName,dayOfWeek,open:'09:00',close:'20:00',isOpen:true}))};
 return new Response(JSON.stringify(data),{status:200,headers:{'Content-Type':'application/json'}});
}
</script>'''
with sync_playwright() as play:
 browser=play.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH') or None,headless=True,args=['--no-sandbox'])
 ctx=browser.new_context(viewport={'width':1280,'height':900},device_scale_factor=1)
 page=ctx.new_page();page.set_default_timeout(4000);page.route('https://**',lambda r:r.abort());errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
 html=re.sub(r'<link[^>]*https://[^>]*>', '', (p/'index.html').read_text()).replace('<script>',customer_mock+'\n<script>',1)
 page.set_content(html,wait_until='domcontentloaded')
 page.wait_for_selector('.slot',state='visible')
 page.locator('.slot').first.click()
 for sel,txt in [('#firstName','דנה'),('#lastName','לוי'),('#phone','0501234567'),('#bookingCode','123456')]:page.locator(sel).fill(txt)
 page.screenshot(path=str(out/'customer-booking-mock.png'),full_page=True,animations='disabled')
 page.locator('#sendBookingCodeBtn').click()
 page.wait_for_function("window.testRequests.some(r=>r.url==='/api/verify/send')")
 page.locator('#confirmBtn').click()
 page.wait_for_function("document.querySelector('#result').textContent.includes('בהצלחה')")
 reqs=page.evaluate('window.testRequests')
 assert next(r for r in reqs if r['url']=='/api/appointments')['body']['code']=='123456',reqs
 page.locator('#myPhone').fill('0501234567');page.locator('#myCode').fill('123456');page.locator('#showMineBtn').click()
 page.wait_for_selector('.appt-row',state='visible')
 assert next(r for r in page.evaluate('window.testRequests') if r['url']=='/api/appointments/mine')['body']['code']=='123456'
 assert not errors,errors
 print('Customer UI JS / mocked API: PASS; verified request payloads')
 admin=ctx.new_page();admin.set_default_timeout(4000);admin.route('https://**',lambda r:r.abort());errors2=[];admin.on('pageerror',lambda e:errors2.append(str(e)))
 html=re.sub(r'<link[^>]*https://[^>]*>', '', (p/'admin.html').read_text()).replace('<script>',admin_mock+'\n<script>',1)
 admin.set_content(html,wait_until='domcontentloaded')
 admin.wait_for_selector('.cal-table')
 assert admin.locator('#dashboard').is_visible()
 admin.screenshot(path=str(out/'admin-dashboard-mock.png'),full_page=True,animations='disabled')
 admin.locator('#slotMin').fill('35');admin.locator('#slotSave').click()
 admin.wait_for_function("document.querySelector('#settingsMsg').textContent.includes('נשמר')")
 requests=admin.evaluate('window.testRequests')
 assert next(r for r in requests if r['url']=='/api/admin/settings/slot')['headers']['X-CSRF-Token']=='demo-csrf',requests
 assert not errors2,errors2
 print('Admin UI JS / mocked API: PASS; CSRF header verified')
 browser.close()
