-- Stable row used to serialize initial administrator registration across instances.
CREATE TABLE IF NOT EXISTS `initial_admin_lock` (`id` int NOT NULL PRIMARY KEY) ENGINE=InnoDB;
INSERT IGNORE INTO `initial_admin_lock` (`id`) VALUES (1);
