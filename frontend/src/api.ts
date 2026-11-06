export type Material = {
  id: string;
  name: string;
  category: string;
  stackSize: number;
  craftable: boolean;
};

export type MaterialLine = {
  materialId: string;
  name: string;
  category: string;
  stackSize: number;
  required: number;
  fromInventory: number;
  missing: number;
  toGather: number;
  craftOperations: number;
  craftedUnits: number;
  surplus: number;
  requiredStacks: { fullStacks: number; remainder: number };
  missingStacks: { fullStacks: number; remainder: number };
};

export type CalculationResult = {
  lines: MaterialLine[];
  issues: { type: string; materialId: string; message: string }[];
  exact: boolean;
};

type CalculationRequest = {
  edition: "JAVA" | "BEDROCK";
  version: string;
  buildQuantity: number;
  requirements: { materialId: string; quantity: number }[];
  inventory: Record<string, number>;
  expandRecipes: boolean;
};

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/+$/, "") ?? "";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  if (import.meta.env.PROD && !apiBaseUrl) {
    throw new Error("Configure a variável VITE_API_BASE_URL com o endereço da API.");
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as
      | { message?: string; errors?: string[] }
      | null;
    throw new Error(body?.errors?.join(" · ") || body?.message || `Erro ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export const api = {
  materials: () => request<Material[]>("/api/catalog/materials"),
  calculate: (body: CalculationRequest) =>
    request<CalculationResult>("/api/calculator", {
      method: "POST",
      body: JSON.stringify(body),
    }),
};
