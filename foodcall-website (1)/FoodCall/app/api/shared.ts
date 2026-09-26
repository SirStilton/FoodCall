import { env } from 'cloudflare:workers';
export const db = () => env.DB as D1Database;
export const validFamily = (s:unknown):s is string => typeof s === 'string' && /^[a-f0-9]{64}$/.test(s);
export const reply = (body:unknown,status=200) => Response.json(body,{status,headers:{'Cache-Control':'no-store'}});
export async function event(family:string,kind:string,detail:string){
 await db().prepare('INSERT INTO events (family,kind,detail,created) VALUES (?,?,?,?)').bind(family,kind,detail,Date.now()).run();
 await db().prepare('DELETE FROM events WHERE family = ? AND id NOT IN (SELECT id FROM events WHERE family = ? ORDER BY id DESC LIMIT 200)').bind(family,family).run();
}
export async function input(req:Request){try{return await req.json() as Record<string,unknown>}catch{return {}}}
