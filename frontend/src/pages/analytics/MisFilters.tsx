import type { MisFilters, MisOptions } from '@/api/misApi';
import { Suppressed } from './MisUi';

export function loadFilters(params: URLSearchParams, options: MisOptions): MisFilters {
  const pickId = (key: string, choices: { id: number }[]) => {
    const selected = Number(params.get(key));
    return choices.some((item) => item.id === selected) ? selected : undefined;
  };
  const fy = params.get('fiscalYear');
  const gender = params.get('gender');
  const stateId = pickId('stateId', options.states);
  const districts = options.districts.filter((district) => !stateId || district.stateId === stateId);
  const districtId = pickId('districtId', districts);
  return {
    fiscalYear: fy && options.financialYears.includes(fy) ? fy : undefined,
    schemeId: pickId('schemeId', options.schemes),
    sectorId: pickId('sectorId', options.sectors),
    stateId: stateId ?? districts.find((district) => district.id === districtId)?.stateId,
    districtId,
    gender: options.genders.find((item) => item.code === gender)?.code,
  };
}

export function FilterStrip({ options, filters, setFilter, reset }: {
  options: MisOptions; filters: MisFilters; setFilter: (key: keyof MisFilters, value: string) => void; reset: () => void;
}) {
  const stateNames = new Map(options.states.map(({ id, name }) => [id, name]));
  const districtOptions = filters.stateId ? options.districts.filter(({ stateId }) => stateId === filters.stateId)
    : options.districts.map((item) => ({ ...item, name: `${item.name} · ${stateNames.get(item.stateId) ?? ''}` }));
  const select = (key: keyof MisFilters, label: string, placeholder: string, items: { id: string | number; name: string }[]) => (
    <div className="min-w-0" key={key}>
      <label className="form-label" htmlFor={`mis-filter-${key}`}>{label}</label>
      <select id={`mis-filter-${key}`} className="form-input" value={filters[key] ?? ''} onChange={(event) => setFilter(key, event.target.value)}>
        <option value="">{placeholder}</option>
        {items.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
      </select>
    </div>
  );
  return (
    <section aria-label="Global reporting filters" className="card z-10 space-y-3 p-4 lg:sticky lg:top-0">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="font-semibold text-navy">Reporting filters</h2>
        <div className="flex flex-wrap items-center gap-3">
          <span className="text-xs text-slate-600">Disclosure threshold k = {options.suppressionThreshold}</span>
          <Suppressed />
          <button className="btn-secondary" type="button" onClick={reset} disabled={!Object.values(filters).some(Boolean)}>Reset filters</button>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-x-3 gap-y-2 sm:grid-cols-3 xl:grid-cols-7">
        {select('fiscalYear', 'Financial year', 'All financial years', options.financialYears.map((year) => ({ id: year, name: year })))}
        {select('schemeId', 'Scheme', 'All schemes', options.schemes)}
        {select('sectorId', 'Sector', 'All sectors', options.sectors)}
        {select('stateId', 'State', 'All states', options.states)}
        {select('districtId', 'District', 'All districts', districtOptions)}
        {select('gender', 'Gender', 'All recorded genders', options.genders.map((gender) => ({ id: gender.code, name: gender.name })))}
        <div className="min-w-0">
          <label className="form-label" htmlFor="mis-filter-social-category">Social category</label>
          <select id="mis-filter-social-category" className="form-input disabled:bg-slate-100 disabled:text-slate-600" aria-describedby="mis-social-category" disabled>
            <option>Unavailable</option>
          </select>
        </div>
      </div>
      <p id="mis-social-category" className="text-xs text-slate-600">Social category is unavailable because the current backend does not record it.</p>
    </section>
  );
}
