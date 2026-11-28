import { readdirSync, readFileSync } from 'node:fs';
import type { McpServer } from '@modelcontextprotocol/server';

const skillsDir = new URL('../plugin/skills/', import.meta.url);

interface Skill {
  name: string;
  description: string;
  body: string;
}

// the skills are Agent Skills (SKILL.md with name and description), the same files the plugin ships
const skills: Skill[] = readdirSync(skillsDir, { withFileTypes: true })
  .filter((entry) => entry.isDirectory())
  .map((entry) => {
    const text = readFileSync(new URL(`${entry.name}/SKILL.md`, skillsDir), 'utf8');
    const [, frontmatter = '', body = ''] = text.split(/^---\n(.*?)\n---\n/ms);
    const field = (key: string) => new RegExp(`^${key}: (.*)$`, 'm').exec(frontmatter)?.[1]?.trim() ?? '';
    return { name: field('name') || entry.name, description: field('description'), body: body.trim() };
  });

const uri = (skill: Skill) => `skill://astro/${skill.name}/SKILL.md`;

export const instructions = [
  'Astro is a Discord bot for temporary voice channels and voice roles. These tools read and change its configuration in the Discord servers the user manages.',
  'Start with guilds_settings_get to learn the ids, change things with the update tools (partial updates, dryRun available) and ask before deleting.',
  'Guides with the full workflow are available as resources and prompts:',
  ...skills.map((skill) => `- ${uri(skill)}: ${skill.description}`),
].join('\n');

export function registerSkills(server: McpServer): void {
  for (const skill of skills) {
    server.registerResource(skill.name, uri(skill), { title: skill.name, description: skill.description, mimeType: 'text/markdown' }, async (resourceUri) => ({
      contents: [{ uri: resourceUri.href, mimeType: 'text/markdown', text: skill.body }],
    }));
    server.registerPrompt(skill.name, { title: skill.name, description: skill.description }, () => ({
      messages: [{ role: 'user' as const, content: { type: 'text' as const, text: skill.body } }],
    }));
  }
}
