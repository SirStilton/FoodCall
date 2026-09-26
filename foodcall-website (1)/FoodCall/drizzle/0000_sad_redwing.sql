CREATE TABLE `calls` (
	`id` text PRIMARY KEY NOT NULL,
	`family` text NOT NULL,
	`meal` text NOT NULL,
	`created` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `idx_calls_family_created` ON `calls` (`family`,`created`);--> statement-breakpoint
CREATE TABLE `devices` (
	`family` text NOT NULL,
	`device` text NOT NULL,
	`name` text NOT NULL,
	`last_seen` integer NOT NULL,
	PRIMARY KEY(`family`, `device`)
);
--> statement-breakpoint
CREATE TABLE `events` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`family` text NOT NULL,
	`kind` text NOT NULL,
	`detail` text NOT NULL,
	`created` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `idx_events_family_id` ON `events` (`family`,`id`);--> statement-breakpoint
CREATE TABLE `responses` (
	`call_id` text NOT NULL,
	`device` text NOT NULL,
	`name` text NOT NULL,
	`minutes` integer NOT NULL,
	`created` integer NOT NULL,
	PRIMARY KEY(`call_id`, `device`)
);
