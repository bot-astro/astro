import type { Plan } from '../data/plans';

export function PlanCard({ plan }: { plan: Plan }) {
  return (
    <article>
      <h3>{plan.name}</h3>
      <p>{plan.description}</p>
      <button title="Choose this plan">Get started</button>
    </article>
  );
}
