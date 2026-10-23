import { useState } from 'react';
import { PlanCard } from './components/PlanCard';
import { plans } from './data/plans';

export default function App() {
  const [query, setQuery] = useState('');
  const [cups, setCups] = useState(0);

  return (
    <main>
      <h1>Track every brew you make</h1>
      <p>BrewLog keeps your coffee experiments in one place.</p>
      <nav aria-label="Main navigation">
        <a href="/journal">Journal</a>
        <a href="/beans">Beans</a>
        <a href="/settings">Settings</a>
      </nav>
      <input
        placeholder="Search your brews…"
        aria-label="Search brews"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <button onClick={() => setCups(cups + 1)}>Log a cup</button>
      <p>{cups === 1 ? 'You logged 1 cup today' : `You logged ${cups} cups today`}</p>
      <section>
        <h2>Pick a plan</h2>
        {plans.map((plan) => (
          <PlanCard key={plan.id} plan={plan} />
        ))}
      </section>
    </main>
  );
}
