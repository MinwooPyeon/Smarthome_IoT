
CREATE TABLE "user" (
	"user_id"	INTEGER		NOT NULL,
	"login_id"	VARCHAR		NULL,
	"password"	VARCHAR		NULL,
	"img"	VARCHAR		NULL,
	"nickname"	VARCHAR		NULL,
	"email"	VARCHAR		NULL,
	"join_date"	timestamptz		NULL,
	"out"	timestamptz		NULL,
	"last_active"	timestamptz		NULL
);

CREATE TABLE "routine_detail" (
	"routine_detail"	INTEGER		NOT NULL,
	"device_id"	INTEGER		NOT NULL,
	"routine_id"	INTEGER		NOT NULL,
	"device_detail"	JSON		NULL
);


CREATE TABLE "gungu" (
	"sgg_code"	INTEGER		NOT NULL,
	"name"	VARCHAR		NULL,
	"sido_code"	INTEGER		NOT NULL
);


CREATE TABLE "hub_device" (
	"hub_device_id"	int		NOT NULL,
	"device_addr"	inet		NULL
);


CREATE TABLE "room" (
	"room_id"	INTEGER		NOT NULL,
	"room_name"	VARCHAR		NULL,
	"created_at"	timestamptz		NULL,
	"user_house_id"	INTEGER		NOT NULL
);


CREATE TABLE "device_positions" (
	"position_id"	INTEGER		NOT NULL,
	"x_coordinate"	DOUBLE PRECISION		NULL,
	"y_coordinate"	DOUBLE PRECISION		NULL,
	"device_id"	INTEGER		NOT NULL
);


CREATE TABLE "routine" (
	"routine_id"	INTEGER		NOT NULL,
	"name"	VARCHAR		NULL,
	"trigger_type"	BOOLEAN		NULL,
	"created_at"	timestamptz		NULL,
	"updated_at"	timestamptz		NULL,
	"routine_weekday"	INTEGER		NULL,
	"routine_description"	TEXT		NULL,
	"user_id"	INTEGER		NOT NULL,
	"act_time"	timestamptz		NULL
);

CREATE TABLE "ir_event_log" (
	"event_id"	bigint		NOT NULL,
	"event_time"	timestamptz		NULL,
	"kind"	text		NULL,
	"ir_device_id"	int		NOT NULL,
	"tx_id"	uuid		NOT NULL
);


CREATE TABLE "eupmyeondong" (
	"emd_code"	INTEGER		NOT NULL,
	"name"	VARCHAR		NULL,
	"sgg_code"	INTEGER		NOT NULL
);


CREATE TABLE "subscribe" (
	"subscribe_id"	INTEGER		NOT NULL,
	"service_id"	INTEGER		NOT NULL,
	"subscribe_date"	timestamptz		NULL,
	"expiration"	timestamptz		NULL,
	"user_id"	INTEGER		NOT NULL
);


CREATE TABLE "floorplans" (
	"floorplan_id"	INTEGER		NOT NULL,
	"image_url"	VARCHAR		NULL,
	"created_at"	timestamptz		NULL,
	"square"	DOUBLE PRECISION		NULL,
	"floorplans_x"	DOUBLE PRECISION		NULL,
	"floorplans_y"	DOUBLE PRECISION		NULL,
	"home_id"	INTEGER		NOT NULL
);


CREATE TABLE "sido" (
	"sido_code"	INTEGER		NOT NULL,
	"name"	VARCHAR		NULL
);


CREATE TABLE "home" (
	"home_id"	INTEGER		NOT NULL,
	"longitude"	DOUBLE PRECISION		NULL,
	"latitude"	DOUBLE PRECISION		NULL,
	"address_id"	INTEGER		NOT NULL
);


CREATE TABLE "services" (
	"service_id"	INTEGER		NOT NULL,
	"tier_id"	INTEGER		NULL,
	"fee"	INTEGER		NULL,
	"interval"	INTEGER		NULL
);


CREATE TABLE "ir_button" (
	"button_id"	INTEGER		NOT NULL,
	"label"	text		NULL,
	"category"	text		NULL,
	"remote_id"	INTEGER		NOT NULL
);


CREATE TABLE "ir_device" (
	"ir_device_id"	int		NOT NULL,
	"device_addr"	int		NULL,
	"hub_device_id"	int		NOT NULL
);


CREATE TABLE "user_home" (
	"user_home_id"	INTEGER		NOT NULL,
	"user_id"	INTEGER		NOT NULL,
	"home_id"	INTEGER		NOT NULL,
	"floorplan_id"	INTEGER		NULL,
	"Field"	VARCHAR(255)		NULL
);


CREATE TABLE "ir_remoteir" (
	"remote_id"	INTEGER		NOT NULL,
	"brand"	text		NULL,
	"model"	text		NULL,
	"device_type"	text		NULL,
	"created_at"	timestamptz		NULL,
	"power consumption"	float		NULL
);


CREATE TABLE "device" (
	"device_id"	INTEGER		NOT NULL,
	"device_name"	VARCHAR		NULL,
	"registered_at"	timestamptz		NULL,
	"device_detail"	JSON		NULL,
	"home_id"	INTEGER		NOT NULL,
	"Field"	VARCHAR(255)		NULL
);


CREATE TABLE "ir_tx_queue" (
	"tx_id"	uuid		NOT NULL,
	"scheduled_at"	timestamptz		NULL,
	"priority"	int		NULL,
	"repeat_count"	int		NULL,
	"interval_ms"	int		NULL,
	"status"	text		NULL,
	"last_error"	text		NULL,
	"created_at"	timestamptz		NULL,
	"signal_id"	int		NOT NULL,
	"ir_device_id"	int		NOT NULL
);


CREATE TABLE "command" (
	"command_id"	INTEGER		NOT NULL,
	"button_id"	INTEGER		NOT NULL,
	"command"	TEXT		NULL,
	"device_id"	INTEGER		NOT NULL,
	"user_id"	INTEGER		NOT NULL
);


CREATE TABLE "addresses" (
	"address_id"	INTEGER		NOT NULL,
	"detail"	VARCHAR		NULL,
	"sgg_code"	INTEGER		NOT NULL,
	"sido_code"	INTEGER		NOT NULL,
	"emd_code"	INTEGER		NOT NULL
);


CREATE TABLE "ir_signal" (
	"signal_id"	INTEGER		NOT NULL,
	"name"	text		NULL,
	"address_code"	bigint		NULL,
	"command_code"	bigint		NULL,
	"sub_code"	bigint		NULL,
	"carrier_hz"	int		NULL,
	"duty_cycle_pct"	real		NULL,
	"frame_count"	int		NULL,
	"frame_len_us"	int		NULL,
	"samples_us"	int[]		NULL,
	"repeat_min"	int		NULL,
	"repeat_max"	int		NULL,
	"repeat_gap_us"	int		NULL,
	"is_toggle"	boolean		NULL,
	"toggle_mask"	BIGINT		NULL,
	"norm_hash"	text		NULL,
	"tolerance_us"	int		NULL,
	"protocol_id"	int		NOT NULL,
	"button_id"	int		NOT NULL
);


CREATE TABLE "ir_protocol" (
	"protocol serial"	INTEGER		NOT NULL,
	"프로토콜 이름"	text		NULL,
	"Field"	text		NULL,
	"true : msb / false : lsb"	boolean		NULL
);

ALTER TABLE "user" ADD CONSTRAINT "PK_USER" PRIMARY KEY (
	"user_id"
);

ALTER TABLE "routine_detail" ADD CONSTRAINT "PK_ROUTINE_DETAIL" PRIMARY KEY (
	"routine_detail"
);

ALTER TABLE "gungu" ADD CONSTRAINT "PK_GUNGU" PRIMARY KEY (
	"sgg_code"
);

ALTER TABLE "hub_device" ADD CONSTRAINT "PK_HUB_DEVICE" PRIMARY KEY (
	"hub_device_id"
);

ALTER TABLE "room" ADD CONSTRAINT "PK_ROOM" PRIMARY KEY (
	"room_id"
);

ALTER TABLE "device_positions" ADD CONSTRAINT "PK_DEVICE_POSITIONS" PRIMARY KEY (
	"position_id"
);

ALTER TABLE "routine" ADD CONSTRAINT "PK_ROUTINE" PRIMARY KEY (
	"routine_id"
);

ALTER TABLE "ir_event_log" ADD CONSTRAINT "PK_IR_EVENT_LOG" PRIMARY KEY (
	"event_id"
);

ALTER TABLE "eupmyeondong" ADD CONSTRAINT "PK_EUPMYEONDONG" PRIMARY KEY (
	"emd_code"
);

ALTER TABLE "subscribe" ADD CONSTRAINT "PK_SUBSCRIBE" PRIMARY KEY (
	"subscribe_id"
);

ALTER TABLE "floorplans" ADD CONSTRAINT "PK_FLOORPLANS" PRIMARY KEY (
	"floorplan_id"
);

ALTER TABLE "sido" ADD CONSTRAINT "PK_SIDO" PRIMARY KEY (
	"sido_code"
);

ALTER TABLE "home" ADD CONSTRAINT "PK_HOME" PRIMARY KEY (
	"home_id"
);

ALTER TABLE "services" ADD CONSTRAINT "PK_SERVICES" PRIMARY KEY (
	"service_id"
);

ALTER TABLE "ir_button" ADD CONSTRAINT "PK_IR_BUTTON" PRIMARY KEY (
	"button_id"
);

ALTER TABLE "ir_device" ADD CONSTRAINT "PK_IR_DEVICE" PRIMARY KEY (
	"ir_device_id"
);

ALTER TABLE "user_home" ADD CONSTRAINT "PK_USER_HOME" PRIMARY KEY (
	"user_home_id"
);

ALTER TABLE "ir_remoteir" ADD CONSTRAINT "PK_IR_REMOTEIR" PRIMARY KEY (
	"remote_id"
);

ALTER TABLE "device" ADD CONSTRAINT "PK_DEVICE" PRIMARY KEY (
	"device_id"
);

ALTER TABLE "ir_tx_queue" ADD CONSTRAINT "PK_IR_TX_QUEUE" PRIMARY KEY (
	"tx_id"
);

ALTER TABLE "command" ADD CONSTRAINT "PK_COMMAND" PRIMARY KEY (
	"command_id"
);

ALTER TABLE "addresses" ADD CONSTRAINT "PK_ADDRESSES" PRIMARY KEY (
	"address_id"
);

ALTER TABLE "ir_signal" ADD CONSTRAINT "PK_IR_SIGNAL" PRIMARY KEY (
	"signal_id"
);

ALTER TABLE "ir_protocol" ADD CONSTRAINT "PK_IR_PROTOCOL" PRIMARY KEY (
	"protocol serial"
);


ALTER TABLE eeum.eupmyeondong    ALTER COLUMN emd_code       ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.home            ALTER COLUMN home_id        ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.floorplans      ALTER COLUMN floorplan_id   ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.routine_detail  ALTER COLUMN routine_detail ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.hub_device      ALTER COLUMN hub_device_id  ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.routine         ALTER COLUMN routine_id     ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.subscribe       ALTER COLUMN subscribe_id   ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.sido            ALTER COLUMN sido_code      ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.services        ALTER COLUMN service_id     ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.room            ALTER COLUMN room_id        ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.gungu           ALTER COLUMN sgg_code       ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.device_positions ALTER COLUMN position_id   ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.ir_button       ALTER COLUMN button_id      ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.ir_device       ALTER COLUMN ir_device_id   ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.ir_remoteir     ALTER COLUMN remote_id      ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.addresses       ALTER COLUMN address_id     ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.command         ALTER COLUMN command_id     ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum."user"          ALTER COLUMN user_id        ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.ir_signal       ALTER COLUMN signal_id      ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.ir_protocol     ALTER COLUMN "protocol serial" ADD GENERATED BY DEFAULT AS IDENTITY;
ALTER TABLE eeum.device ALTER COLUMN device_id ADD GENERATED BY DEFAULT AS IDENTITY;