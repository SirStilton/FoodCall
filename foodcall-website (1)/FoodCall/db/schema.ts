import { sqliteTable, text, integer, primaryKey, index } from "drizzle-orm/sqlite-core";
export const calls = sqliteTable("calls", {
  id: text("id").primaryKey(), family: text("family").notNull(), meal: text("meal").notNull(), created: integer("created").notNull(),
}, t => [index("idx_calls_family_created").on(t.family,t.created)]);
export const responses = sqliteTable("responses", {
  callId: text("call_id").notNull(), device: text("device").notNull(), name: text("name").notNull(), minutes: integer("minutes").notNull(), created: integer("created").notNull(),
}, t => [primaryKey({columns:[t.callId,t.device]})]);
export const devices = sqliteTable("devices", {
  family: text("family").notNull(), device: text("device").notNull(), name: text("name").notNull(), lastSeen: integer("last_seen").notNull(), fcmToken: text("fcm_token"),
}, t => [primaryKey({columns:[t.family,t.device]})]);
export const events = sqliteTable("events", {
  id: integer("id").primaryKey({autoIncrement:true}), family: text("family").notNull(), kind: text("kind").notNull(), detail: text("detail").notNull(), created: integer("created").notNull(),
}, t => [index("idx_events_family_id").on(t.family,t.id)]);
