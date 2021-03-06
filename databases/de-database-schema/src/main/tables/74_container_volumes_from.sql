SET search_path = public, pg_catalog;

--
-- Contains records of which containers to mount volumes from. This is
-- associated with a container_settings record since this is done on a per-tool
-- basis rather than on a per-image basis.
--
CREATE TABLE container_volumes_from (
  -- primary key
  id uuid UNIQUE NOT NULL DEFAULT uuid_generate_v1(),

  -- foreign key into the container_settings_table.
  container_settings_id uuid NOT NULL,

  -- The name of the container to mount volumes from
  name text NOT NULL,

  unique(container_settings_id, name)
)
