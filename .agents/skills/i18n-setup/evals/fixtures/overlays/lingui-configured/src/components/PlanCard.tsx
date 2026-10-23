import { Trans, useLingui } from '@lingui/react/macro';
import type { Plan } from '../data/plans';

export function PlanCard({ plan }: { plan: Plan }) {
  const { i18n, t } = useLingui();

  return (
    <article>
      <h3>{i18n._(plan.name)}</h3>
      <p>{i18n._(plan.description)}</p>
      <button title={t`Choose this plan`}>
        <Trans>Get started</Trans>
      </button>
    </article>
  );
}
