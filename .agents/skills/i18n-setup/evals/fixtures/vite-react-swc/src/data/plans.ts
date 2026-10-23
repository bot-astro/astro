export interface Plan {
  id: string;
  name: string;
  description: string;
}

export const plans: Plan[] = [
  {
    id: 'free',
    name: 'Home barista',
    description: 'Log up to 20 brews a month and keep simple tasting notes.',
  },
  {
    id: 'pro',
    name: 'Cafe pro',
    description: 'Unlimited brews, recipe sharing, and water chemistry tracking.',
  },
];
