import { msg } from '@lingui/core/macro';
import type { MessageDescriptor } from '@lingui/core';

export interface Plan {
  id: string;
  name: MessageDescriptor;
  description: MessageDescriptor;
}

export const plans: Plan[] = [
  {
    id: 'free',
    name: msg`Home barista`,
    description: msg`Log up to 20 brews a month and keep simple tasting notes.`,
  },
  {
    id: 'pro',
    name: msg`Cafe pro`,
    description: msg`Unlimited brews, recipe sharing, and water chemistry tracking.`,
  },
];
