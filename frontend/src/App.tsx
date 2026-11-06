import { useEffect, useMemo, useState } from "react";
import {
  ArrowDown,
  ArrowRight,
  Blocks,
  BookOpen,
  Check,
  ChevronDown,
  CircleHelp,
  Compass,
  Hammer,
  Minus,
  Package,
  Plus,
  Search,
  Sparkles,
  Trash2,
  X,
} from "lucide-react";
import { api, type CalculationResult, type Material } from "./api";

type Selection = { quantity: number; inventory: number };
type SelectionMap = Record<string, Selection>;

const iconFor: Record<string, string> = {
  oak_log: "▥",
  oak_planks: "▦",
  stick: "╱",
  crafting_table: "▤",
  chest: "▣",
  cobblestone: "◆",
  furnace: "▧",
  coal: "●",
  torch: "♨",
  iron_ingot: "⬟",
  iron_pickaxe: "⚒",
};

const imageFor: Record<string, string> = {
  oak_log: new URL("../img/stripped_log_oak.png", import.meta.url).href,
  oak_planks: new URL("../img/bloco_madeira_minecraft_sem_fundo.png", import.meta.url).href,
  stick: new URL("../img/graveto_minecraft_sem_fundo.png", import.meta.url).href,
  crafting_table: new URL("../img/bancada_craft_minecraft_sem_fundo.png", import.meta.url).href,
  chest: new URL("../img/baú_minecraft_sem_fundo.png", import.meta.url).href,
  cobblestone: new URL("../img/cobblestone.png", import.meta.url).href,
  furnace: new URL("../img/furnace.png", import.meta.url).href,
  coal: new URL("../img/Coal_JE4_BE3.png", import.meta.url).href,
  torch: new URL("../img/tocha_minecraft_sem_fundo.png", import.meta.url).href,
  iron_ingot: new URL("../img/lingote_ferro_minecraft_sem_fundo.png", import.meta.url).href,
  iron_pickaxe: new URL("../img/pickaxe.png", import.meta.url).href,
};

const steveImage = new URL("../img/steve.png", import.meta.url).href;

const colorFor: Record<string, string> = {
  oak_log: "wood",
  oak_planks: "planks",
  stick: "stick",
  crafting_table: "table",
  chest: "chest",
  cobblestone: "stone",
  furnace: "furnace",
  coal: "coal",
  torch: "torch",
  iron_ingot: "iron",
  iron_pickaxe: "pickaxe",
};

function MaterialArtwork({ materialId }: { materialId: string }) {
  const image = imageFor[materialId];

  return image
    ? <img src={image} alt="" aria-hidden="true" />
    : <span aria-hidden="true">{iconFor[materialId] ?? "◆"}</span>;
}

function formatNumber(value: number) {
  return new Intl.NumberFormat("pt-BR").format(value);
}

function App() {
  const [materials, setMaterials] = useState<Material[]>([]);
  const [selection, setSelection] = useState<SelectionMap>({});
  const [result, setResult] = useState<CalculationResult | null>(null);
  const [search, setSearch] = useState("");
  const [activeCategory, setActiveCategory] = useState("Todos");
  const [edition, setEdition] = useState<"JAVA" | "BEDROCK">("JAVA");
  const [version, setVersion] = useState("1.21.4");
  const [buildQuantity, setBuildQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [calculating, setCalculating] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    api.materials()
      .then(setMaterials)
      .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : "Não foi possível carregar o catálogo."))
      .finally(() => setLoading(false));
  }, []);

  const categories = useMemo(
    () => ["Todos", ...new Set(materials.map((material) => material.category))],
    [materials],
  );
  const filtered = useMemo(
    () =>
      materials.filter((material) => {
        const matchesCategory = activeCategory === "Todos" || material.category === activeCategory;
        const matchesSearch = `${material.name} ${material.id} ${material.category}`
          .toLocaleLowerCase("pt-BR")
          .includes(search.toLocaleLowerCase("pt-BR"));
        return matchesCategory && matchesSearch;
      }),
    [activeCategory, materials, search],
  );

  const chosenMaterials = useMemo(
    () => materials.filter((material) => selection[material.id]),
    [materials, selection],
  );
  const requiredMaterials = useMemo(
    () => chosenMaterials.filter((material) => selection[material.id].quantity > 0),
    [chosenMaterials, selection],
  );
  const selectedUnits = Object.values(selection).reduce((total, item) => total + item.quantity, 0);

  function updateSelection(id: string, field: keyof Selection, value: number) {
    setSelection((current) => {
      const existing = current[id];
      if (!existing) return current;
      const updated = {
        ...current,
        [id]: { ...existing, [field]: Math.min(10_000_000, Math.max(0, Math.trunc(value))) },
      };
      if (updated[id].quantity === 0 && updated[id].inventory === 0) delete updated[id];
      return updated;
    });
    setResult(null);
  }

  function addMaterial(material: Material) {
    setSelection((current) => ({
      ...current,
      [material.id]: {
        quantity: (current[material.id]?.quantity ?? 0) + 1,
        inventory: current[material.id]?.inventory ?? 0,
      },
    }));
    setResult(null);
  }

  function removeMaterial(id: string) {
    setSelection((current) => {
      const updated = { ...current };
      delete updated[id];
      return updated;
    });
    setResult(null);
  }

  async function calculate() {
    if (requiredMaterials.length === 0) return;
    setError("");
    setCalculating(true);
    try {
      const response = await api.calculate({
        edition,
        version,
        buildQuantity,
        requirements: requiredMaterials.map((material) => ({
          materialId: material.id,
          quantity: selection[material.id].quantity,
        })),
        inventory: Object.fromEntries(
          chosenMaterials.map((material) => [material.id, selection[material.id].inventory]),
        ),
        expandRecipes: true,
      });
      setResult(response);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Falha ao calcular os materiais.");
    } finally {
      setCalculating(false);
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#top" aria-label="Craftbook, início">
          <span className="brand-mark"><Blocks size={20} strokeWidth={2.5} /></span>
          <span>craft<span className="brand-light">book</span><small>BUILD PLANNER</small></span>
        </a>
        <div className="nav-caption">WORKSPACE</div>
        <nav className="side-nav" aria-label="Navegação principal">
          <a className="nav-link active" href="#catalog"><Compass size={17} />Explorar catálogo<span className="nav-dot" /></a>
          <a className="nav-link" href="#plan"><Blocks size={17} />Meu planejamento</a>
          <a className="nav-link" href="#results"><BookOpen size={17} />Lista de materiais</a>
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-tip">
            <span className="tip-icon"><Sparkles size={16} /></span>
            <strong>Uma ideia de cada vez.</strong>
            <p>Adicione blocos ao plano. A gente cuida das contas.</p>
            <a href="#catalog">Começar <ArrowRight size={13} /></a>
          </div>
          <div className="profile">
            <div className="profile-avatar">C</div>
            <span><strong>Calculadora de blocos</strong><small>Projetos salvos: em breve</small></span>
            <CircleHelp size={17} className="profile-help" />
          </div>
        </div>
      </aside>

      <main className="main-content" id="top">
        <header className="topbar">
          <div className="breadcrumbs"><span>Workspace</span><span className="crumb-slash">/</span><strong>Calculadora</strong></div>
          <div className="topbar-right">
            <span className={`status-pill ${error && !materials.length ? "offline" : ""}`}><span />{loading ? "Conectando API" : error && !materials.length ? "API indisponível" : "API conectada"}</span>
            <span className="topbar-divider" />
            <button className="avatar-button" aria-label="Perfil">W</button>
          </div>
        </header>

        <div className="page-wrap">
          <section className="hero">
            <div className="hero-copy">
              <div className="eyebrow"><span className="eyebrow-line" />SEU PRÓXIMO PROJETO COMEÇA AQUI</div>
              <h1>Planeje. Construa.<br /><span>Sem faltar bloco.</span></h1>
              <p>Monte sua lista, confira as receitas e vá para o mundo sabendo exatamente o que levar.</p>
              <a className="hero-link" href="#catalog">Explorar materiais <ArrowDown size={15} /></a>
            </div>
            <div className="hero-art" aria-hidden="true">
              <span className="pixel-cloud cloud-one" />
              <span className="pixel-cloud cloud-two" />
              <span className="pixel-sun" />
              <span className="block-mountain mountain-back" />
              <span className="block-mountain mountain-front" />
              <span className="block-tree tree-one"><i /><b /></span>
              <span className="block-tree tree-two"><i /><b /></span>
              <span className="land-layer land-back" />
              <span className="land-layer land-front" />
              <img className="hero-character" src={steveImage} alt="" />
              <span className="scene-flower flower-one">✿</span>
              <span className="scene-flower flower-two">✿</span>
              <span className="art-caption"><span className="art-caption-dot" />UM MUNDO DE POSSIBILIDADES</span>
            </div>
            <div className="hero-index">01 <span>/</span> 03</div>
          </section>

          <section className="stat-strip" aria-label="Resumo do catálogo">
            <div className="stat-item"><span className="stat-icon green"><Package size={17} /></span><span><strong>{loading ? "—" : formatNumber(materials.length)}</strong><small>materiais no catálogo</small></span></div>
            <div className="stat-separator" />
            <div className="stat-item"><span className="stat-icon gold"><Hammer size={17} /></span><span><strong>{loading ? "—" : formatNumber(materials.filter((material) => material.craftable).length)}</strong><small>com receita de criação</small></span></div>
            <div className="stat-separator" />
            <div className="stat-note"><span className="tiny-spark">✳</span> Receitas para <strong>Minecraft Java</strong> e <strong>Bedrock</strong></div>
          </section>

          <div className="section-heading" id="catalog">
            <div>
              <div className="eyebrow compact"><span className="eyebrow-line" />MONTE SUA LISTA</div>
              <h2>O que vamos construir?</h2>
              <p>Escolha os materiais do seu projeto. As receitas são expandidas automaticamente.</p>
            </div>
            <div className="edition-control">
              <span>EDIÇÃO</span>
              <button className="edition-select" onClick={() => setEdition(edition === "JAVA" ? "BEDROCK" : "JAVA")}>
                {edition === "JAVA" ? "Java Edition" : "Bedrock Edition"}<ChevronDown size={15} />
              </button>
            </div>
          </div>

          {error && <div className="error-banner" role="alert"><span>{error}</span><button onClick={() => setError("")} aria-label="Fechar"><X size={15} /></button></div>}

          <div className="workspace-grid">
            <section className="catalog-panel">
              <div className="catalog-toolbar">
                <label className="search-box">
                  <Search size={16} />
                  <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar materiais..." aria-label="Buscar materiais" />
                  {search && <button onClick={() => setSearch("")} aria-label="Limpar busca"><X size={14} /></button>}
                </label>
                <span className="catalog-count">{filtered.length} ITENS</span>
              </div>
              <div className="category-list" role="group" aria-label="Filtrar por categoria">
                {categories.map((category) => (
                  <button key={category} className={`category-chip ${activeCategory === category ? "selected" : ""}`} onClick={() => setActiveCategory(category)}>
                    {category}
                  </button>
                ))}
              </div>
              {loading ? (
                <div className="empty-state"><span className="loading-spinner" />Carregando materiais do catálogo…</div>
              ) : filtered.length ? (
                <div className="material-grid">
                  {filtered.map((material) => {
                    const count = selection[material.id]?.quantity ?? 0;
                    return (
                      <button key={material.id} className={`material-card ${count ? "in-plan" : ""}`} onClick={() => addMaterial(material)} aria-label={`Adicionar ${material.name}${count ? `, ${count} selecionado` : ""}`}>
                        <span className={`item-art ${colorFor[material.id] ?? "stone"}`}><MaterialArtwork materialId={material.id} /></span>
                        <span className="material-meta"><span className="material-category">{material.category}</span><strong>{material.name}</strong><small>Stack de {material.stackSize}</small></span>
                        <span className="material-add">{count ? <><Check size={14} /><b>{count}</b></> : <Plus size={16} />}</span>
                      </button>
                    );
                  })}
                </div>
              ) : (
                <div className="empty-state"><Search size={18} />Nenhum material encontrado. Tente outra busca.</div>
              )}
              <div className="catalog-footer"><span>CATÁLOGO DE MATERIAIS</span><span>Uma base inicial para começar a planejar.</span></div>
            </section>

            <aside className="planner-panel" id="plan">
              <div className="planner-head">
                <div><div className="panel-kicker">RASCUNHO ATUAL</div><h3>Lista de materiais</h3></div>
                <div className="list-count">{chosenMaterials.length.toString().padStart(2, "0")}</div>
              </div>
              <div className="project-name"><span className="project-marker"><Blocks size={16} /></span><span><strong>Plano sem nome</strong><small>Temporário · não salvo</small></span><button aria-label="Editar nome do projeto" title="Projetos salvos em breve"><ChevronDown size={15} /></button></div>
              <div className="quantity-row">
                <span>Quantidade de construções</span>
                <div className="stepper">
                  <button onClick={() => { setBuildQuantity((value) => Math.max(1, value - 1)); setResult(null); }} aria-label="Diminuir construções"><Minus size={13} /></button>
                  <strong>{buildQuantity}</strong>
                  <button onClick={() => { setBuildQuantity((value) => Math.min(1000, value + 1)); setResult(null); }} aria-label="Aumentar construções"><Plus size={13} /></button>
                </div>
              </div>
              <div className="planner-divider" />
              <div className="list-label-row"><span>ITENS DA CONSTRUÇÃO</span><span>QUANTIDADE</span></div>

              {chosenMaterials.length ? (
                <div className="selected-list">
                  {chosenMaterials.map((material) => (
                    <div className="selected-item" key={material.id}>
                      <span className={`mini-art ${colorFor[material.id] ?? "stone"}`}><MaterialArtwork materialId={material.id} /></span>
                      <span className="selected-name"><strong>{material.name}</strong><small>Tenho no inventário</small></span>
                      <input className="inventory-input" aria-label={`Quantidade de ${material.name} no inventário`} type="number" min="0" max="10000000" step="1" value={selection[material.id].inventory} onChange={(event) => updateSelection(material.id, "inventory", Number(event.target.value))} />
                      <div className="selected-stepper">
                        <button onClick={() => updateSelection(material.id, "quantity", selection[material.id].quantity - 1)} aria-label={`Diminuir ${material.name}`}><Minus size={12} /></button>
                        <span>{selection[material.id].quantity}</span>
                        <button onClick={() => updateSelection(material.id, "quantity", selection[material.id].quantity + 1)} aria-label={`Aumentar ${material.name}`}><Plus size={12} /></button>
                      </div>
                      <button className="remove-item" onClick={() => removeMaterial(material.id)} aria-label={`Remover ${material.name}`}><Trash2 size={14} /></button>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="planner-empty"><span className="empty-cube"><Blocks size={19} /></span><strong>Seu projeto começa com uma ideia.</strong><p>Adicione materiais do catálogo para criar sua lista.</p><a href="#catalog">Escolher materiais <ArrowRight size={13} /></a></div>
              )}

              <div className="planner-bottom">
                <div className="plan-totals"><span>Itens no plano <strong>{formatNumber(selectedUnits)}</strong></span><span>Versão do jogo <strong className="version-control"><input aria-label="Versão do Minecraft" value={version} onChange={(event) => { setVersion(event.target.value); setResult(null); }} maxLength={14} /></strong></span></div>
                <button className="calculate-button" onClick={calculate} disabled={!requiredMaterials.length || calculating}>
                  {calculating ? <><span className="button-spinner" />Calculando receitas…</> : <>Calcular materiais <ArrowRight size={16} /></>}
                </button>
                <div className="privacy-note"><span />Cálculo baseado nas receitas do catálogo</div>
              </div>
            </aside>
          </div>

          <section className="results-section" id="results">
            <div className="results-heading">
              <div><div className="eyebrow compact"><span className="eyebrow-line" />PRONTO PARA A EXPEDIÇÃO?</div><h2>Resumo da coleta</h2></div>
              {result && <span className={`result-status ${result.exact ? "complete" : "partial"}`}><span />{result.exact ? "Cálculo completo" : "Estimativa parcial"}</span>}
            </div>
            {result ? (
              <>
                {!result.exact && <div className="issue-note"><CircleHelp size={15} /><span>Algumas receitas não estão cadastradas para esta edição ou versão. Os itens aparecem como materiais a obter.</span></div>}
                <div className="result-table">
                  <div className="result-row result-header"><span>MATERIAL</span><span>NECESSÁRIO</span><span>NO INVENTÁRIO</span><span>FALTAM</span><span>FABRICAÇÃO</span></div>
                  {result.lines.map((line) => (
                    <div className="result-row" key={line.materialId}>
                      <span className="result-material"><span className={`mini-art ${colorFor[line.materialId] ?? "stone"}`}><MaterialArtwork materialId={line.materialId} /></span><strong>{line.name}</strong></span>
                      <span>{formatNumber(line.required)}</span>
                      <span className="inventory-value">{formatNumber(line.fromInventory)}</span>
                      <span className={line.missing > 0 ? "missing-value" : "inventory-value"}>{formatNumber(line.missing)}</span>
                      <span>{line.craftOperations ? `${formatNumber(line.craftOperations)} ${line.craftOperations === 1 ? "operação" : "operações"}${line.surplus ? ` · +${line.surplus}` : ""}` : line.toGather ? "Coletar" : "—"}</span>
                    </div>
                  ))}
                </div>
                <div className="result-footnote"><Sparkles size={14} /><span>As sobras de uma receita são informadas, mas não reaproveitadas em outros materiais.</span></div>
              </>
            ) : (
              <div className="results-placeholder"><span className="placeholder-icon"><Package size={19} /></span><span><strong>Os materiais para sua aventura vão aparecer aqui.</strong><small>Adicione itens ao projeto e calcule para ver o que falta.</small></span><span className="placeholder-arrow"><ArrowRight size={17} /></span></div>
            )}
          </section>

          <footer className="page-footer">
            <span>CRAFTBOOK <span className="footer-dot">✳</span> FEITO PARA CONSTRUIR</span>
            <span>Dados do catálogo carregados da sua instância local.</span>
          </footer>
        </div>
      </main>
    </div>
  );
}

export default App;
