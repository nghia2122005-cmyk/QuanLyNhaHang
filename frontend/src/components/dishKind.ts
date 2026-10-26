// The menu has no picture or category field, so the illustration is a display-only guess from the dish name.
export type DishKind = 'peachTea' | 'tea' | 'milkCoffee' | 'coffee' | 'pho' | 'bun' | 'brokenRice' | 'rice' | 'springRoll' | 'bowl'

const fold = (text: string) => text.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/đ/g, 'd').toLowerCase()

const rules: [DishKind, RegExp][] = [
  ['peachTea', /tra dao|tra vai|tra trai cay/],
  ['milkCoffee', /ca phe sua|cafe sua|bac xiu|nau da/],
  ['coffee', /ca phe|cafe|coffee|espresso/],
  ['tea', /tra|nuoc|sinh to|soda|chanh|da me|sua/],
  ['pho', /pho|mi |hu tieu/],
  ['bun', /bun|mien/],
  ['brokenRice', /com tam|suon/],
  ['rice', /com/],
  ['springRoll', /goi cuon|cuon|nem/],
]

export const dishBackgrounds: Record<DishKind, string> = {
  peachTea: '#FDE8D7', tea: '#E6F2DF', milkCoffee: '#F3E9DC', coffee: '#EFE4DA', pho: '#FBEFD9',
  bun: '#E3F1EF', brokenRice: '#FBE6D4', rice: '#FFF4D6', springRoll: '#E3F1E6', bowl: '#EEF7F6',
}

export function dishKind(name: string): DishKind {
  const folded = ` ${fold(name)} `
  return rules.find(([, pattern]) => pattern.test(folded))?.[0] ?? 'bowl'
}

/** Background tint matching a dish illustration, for larger tiles (menu grid). */
export const dishBackground = (name: string) => dishBackgrounds[dishKind(name)]
