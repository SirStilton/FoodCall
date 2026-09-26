import {db,validFamily,reply,event,input} from '../shared';
import {sendFoodReady} from '../fcm';
export async function POST(req:Request){
 const p=await input(req); if(!validFamily(p.family)||!['BREAKFAST','LUNCH','DINNER','SNACK'].includes(String(p.meal)))return reply({error:'Ungültiger Call'},400);
 const id=crypto.randomUUID(), now=Date.now();
 try{
  await db().prepare('INSERT INTO calls(id,family,meal,created) VALUES (?,?,?,?)').bind(id,p.family,p.meal,now).run();
  await event(p.family,'call',String(p.meal));
  const push=await sendFoodReady(p.family,id,String(p.meal));
  if(push.failed)await event(p.family,'push',`${push.sent} Push gesendet, ${push.failed} fehlgeschlagen`);
  return reply({id,created:now,pushSent:push.sent});
 }catch{return reply({error:'Senden fehlgeschlagen'},503)}
}
