import {input} from '../../shared';
import {adminCookie,validPassword,loginBucket,failedLoginCount,recordFailedLogin} from '../auth';
export async function POST(req:Request){
 try{
  const bucket=await loginBucket(req);
  if(await failedLoginCount(bucket)>=5)return Response.json({error:'Zu viele Versuche. Bitte in 15 Minuten erneut probieren.'},{status:429,headers:{'Cache-Control':'no-store','Retry-After':'900'}});
  const p=await input(req);
  if(!validPassword(p.password)){
   await recordFailedLogin(bucket);
   return Response.json({error:'Passwort stimmt nicht'},{status:401,headers:{'Cache-Control':'no-store'}});
  }
  return Response.json({ok:true},{headers:{'Set-Cookie':await adminCookie(),'Cache-Control':'no-store'}});
 }catch{return Response.json({error:'Anmeldung vorübergehend nicht verfügbar'},{status:503,headers:{'Cache-Control':'no-store'}})}
}
