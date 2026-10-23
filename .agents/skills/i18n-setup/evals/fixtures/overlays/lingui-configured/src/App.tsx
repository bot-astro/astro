import { useState } from 'react';
import { Trans, Plural, useLingui } from '@lingui/react/macro';
import { PlanCard } from './components/PlanCard';
import { plans } from './data/plans';

export default function App() {
  const { t } = useLingui();
  const [query, setQuery] = useState('');
  const [cups, setCups] = useState(0);

  return (
    <main>
      <h1>
        <Trans>Track every brew you make</Trans>
      </h1>
      <p>
        <Trans>BrewLog keeps your coffee experiments in one place.</Trans>
      </p>
      <nav aria-label={t`Main navigation`}>
        <a href="/journal">
          <Trans>Journal</Trans>
        </a>
        <a href="/beans">
          <Trans>Beans</Trans>
        </a>
        <a href="/settings">
          <Trans>Settings</Trans>
        </a>
      </nav>
      <input
        placeholder={t`Search your brews…`}
        aria-label={t`Search brews`}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <button onClick={() => setCups(cups + 1)}>
        <Trans>Log a cup</Trans>
      </button>
      <p>
        <Plural value={cups} one="You logged # cup today" other="You logged # cups today" />
      </p>
      <section>
        <h2>
          <Trans>Pick a plan</Trans>
        </h2>
        {plans.map((plan) => (
          <PlanCard key={plan.id} plan={plan} />
        ))}
      </section>
    </main>
  );
}
