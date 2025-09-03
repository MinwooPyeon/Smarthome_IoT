CREATE TABLE "userdetail" (
	"detail_id"	INTEGER		NOT NULL,
	"user_address"	VARCHAR		NULL,
	"user_floorplans"	VARCHAR		NULL,
	"floorplans_x"	DOUBLE PRECISION		NULL,
	"floorplans_y"	DOUBLE PRECISION		NULL,
	"user_id"	INTEGER		NOT NULL
);

ALTER TABLE "userdetail" ADD CONSTRAINT "PK_USERDETAIL" PRIMARY KEY (
	"detail_id"
);

ALTER TABLE "userdetail" ADD CONSTRAINT "FK_user_TO_userdetail_1" FOREIGN KEY (
	"user_id"
)
REFERENCES "user" (
	"user_id"
);