import {db,validFamily,reply} from '../shared';
export async function GET(req:Request){
 const family=new URL(req.url).searchParams.get('family');
 if(!validFamily(family))return reply({error:'Ungültige Familie'},400);
 try{
 const call=await db().prepare('SELECT id,meal,created FROM calls WHERE family=? ORDER BY created DESC LIMIT 1').bind(family).first<{id:string,meal:string,created:number}>();
 const responses=call?(await db().prepare('SELECT name,minutes,device,created FROM responses WHERE call_id=? ORDER BY created DESC').bind(call.id).all<{name:string,minutes:number,device:string,created:number}>()).results:[];
 let replyUntil=call?.created?call.created+10*60_000:0;
 if(call&&replyUntil>Date.now()&&responses.length){
  const active=(await db().prepare('SELECT device FROM devices WHERE family=? AND last_seen>?').bind(family,call.created-30_000).all<{device:string}>()).results;
  if(active.length&&active.every(d=>responses.some(r=>r.device===d.device))){
   replyUntil=Math.min(replyUntil,Math.max(...responses.map(r=>r.created+r.minutes*60_000)));
  }
 }
 const test=await db().prepare("SELECT MAX(id) AS id FROM events WHERE family=? AND kind='test'").bind(family).first<{id:number|null}>();
 return reply({call,responses,replyUntil,testId:test?.id||0});
 }catch{return reply({error:'Verbindung vorübergehend nicht verfügbar'},503)}
}
