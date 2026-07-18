-- V2__collection_folder_nesting.sql
-- Adds folder nesting to collections (a "folder" is just a Collection whose
-- parent_id points at another Collection) and explicit ordering within a level.
-- See APIFORGE_2.0_MASTER_ARCHITECTURE.md §14 -- this was previously a flat list
-- with no nesting at all.

ALTER TABLE collections ADD COLUMN parent_id BIGINT REFERENCES collections (id) ON DELETE CASCADE;
ALTER TABLE collections ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_collections_parent_id ON collections (parent_id);
