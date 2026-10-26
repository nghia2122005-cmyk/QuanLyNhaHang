export interface MenuItem { id: number; name: string; price: number; status: 'ON_SALE' | 'DISCONTINUED' }
export interface SaveMenuItem { name: string; price: number }
export interface IngredientOption { id: number; name: string; unit: string; status: 'ACTIVE' | 'INACTIVE' }
export interface RecipeLine { ingredientId: number; ingredientName: string; unit: string; quantityPerServing: number }
export interface SaveRecipeLine { ingredientId: number; quantityPerServing: number }
