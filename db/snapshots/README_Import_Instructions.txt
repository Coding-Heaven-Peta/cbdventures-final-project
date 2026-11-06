CBDVENTURES — PHASE 4 DATABASE HANDOVER
======================================

Purpose
-------
This folder contains the current CBDVentures MySQL database checkpoint, trigger definitions, and recovery snapshots. It supports the database handover only; it is not the written Phase 4 report.

FILES
-----
1. cbdventures_db_final_checkpoint.sql
   Latest SQLPro export of the database structure and data, saved after the database checks.

2. cbdventures_db_triggers_backup_all_six.sql
   Separate definitions for the six triggers listed below. Check the restored database before importing; do not create duplicate triggers.

3. cbdventures_db_before_real_listings.sql
   Original recovery snapshot from before the real-business listing changes. Keep unchanged.

4. cbdventures_db_after_real_listings.sql
   Intermediate checkpoint. Useful if you need to compare or recover an earlier stage.

DATABASE INVENTORY AT HANDOVER
------------------------------
Table                         Rows
user                             4
category                         5
business                         3
business_hours                  21
amenity                          5
business_amenity                 1
business_image                   0
event                            0
review                           0
user_preference                  0
user_preference_category         0
user_activity                    0
Total                           39

TRIGGERS
--------
- trg_business_within_1km_insert
- trg_business_within_1km_update
- trg_event_future_insert
- trg_event_future_update
- trg_review_no_self_review
- trg_review_24h_limit

Checks performed in SQLPro reported that the radius triggers rejected test locations outside 1 km, event triggers rejected the tested past/upcoming cases, the review triggers rejected the tested self-review and repeat-review attempts, and the test transactions were rolled back. All six triggers appeared in INFORMATION_SCHEMA.TRIGGERS. Relationship checks performed returned zero orphan rows for the relationships queried.

RESTORE NOTES
-------------
1. Do not overwrite the live database to test a restore. Use a new, empty test schema first.
2. Import cbdventures_db_final_checkpoint.sql into the intended empty schema using a MySQL-compatible SQL tool.
3. Check INFORMATION_SCHEMA.TRIGGERS for that schema. If the six triggers already exist, do not import the trigger file again.
4. If the triggers are missing, select the target schema and run cbdventures_db_triggers_backup_all_six.sql with a MySQL script runner that supports the DELIMITER directive. The trigger definitions depend on the business, event, and review tables being present.
5. Verify the table row counts and trigger names after restoration.
6. Keep the original and intermediate backups unchanged. Do not execute the trigger backup against the current live database while those triggers already exist.

DATA QUALITY / LIMITATIONS TO PASS ON
------------------------------------
- The three business records are SA Queen Hair & Beauty, Black Bistro Lounge & Cocktail Kitchen, and Es'kia Mphahlele Community Library.
- All three currently have owner_id = NULL and price_level = NULL. Do not assign owners or prices without verification.
- The library's Free Wi-Fi association was retained provisionally; it should be confirmed before presenting it as verified.
- The three stored business coordinates calculate within 1 km of the reference point hard-coded in the triggers: latitude -25.746400, longitude 28.192483. This confirms the stored-coordinate calculation only; exact business entrances and source details still require final verification.
- The event and review tables are empty. The empty state is expected at the end of the test transactions.
- Price levels use the current numeric mapping: 1 = R (lower), 2 = RR (middle), 3 = RRR (higher); NULL means unverified/not set.
- SQL exports and this handover may contain user records. Share only with the project group/lecturer through an approved channel; do not post publicly.

HANDOVER CHECKLIST
------------------
[ ] Place the final checkpoint and trigger backup in the handover folder.
[ ] Keep the original and intermediate backups in a Backups subfolder.
[ ] Include this README_Import_Instructions.txt file.
[ ] Confirm the final checkpoint file opens and is not zero bytes.
[ ] Share the folder or a ZIP with the group using an approved channel.
[ ] Tell the group to test restoration in a separate schema before relying on it.

Prepared as operational handover notes. This README does not replace the group's written Phase 4 report or the final verification of external business details.
