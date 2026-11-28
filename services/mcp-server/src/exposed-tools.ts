import type { Tool } from './tools/_/tool.js';

/**
 * Tools an agent gets: read a server's configuration and change it.
 * Billing, premium, media, the setup wizard, the user's email and the cache and error log resets stay out.
 */
const EXPOSED = new Set([
  'discord_guilds_list',
  'discord_guilds_listChannels',
  'discord_guilds_listRoles',
  'discord_constants_listChannelPermissions',
  'guilds_settings_get',
  'guilds_settings_update',
  'guilds_settings_updateProfile',
  'guilds_updatesChannel_update',
  'guilds_generators_create',
  'guilds_generators_update',
  'guilds_generators_delete',
  'guilds_interfaces_create',
  'guilds_interfaces_update',
  'guilds_interfaces_delete',
  'guilds_interfaces_move',
  'guilds_connections_create',
  'guilds_connections_update',
  'guilds_connections_delete',
  'guilds_templates_create',
  'guilds_templates_update',
  'guilds_templates_delete',
  'guilds_errors_list',
]);

export function exposedTools(all: readonly Tool[]): Tool[] {
  const tools = all.filter((tool) => EXPOSED.has(tool.name));
  const missing = [...EXPOSED].filter((name) => !tools.some((tool) => tool.name === name));
  if (missing.length > 0) {
    throw new Error(`Tools not in the generated set: ${missing.join(', ')}`);
  }
  return tools;
}
