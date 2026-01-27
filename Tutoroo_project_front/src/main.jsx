/** @jsxImportSource @emotion/react */

import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { Global } from "@emotion/react";
import "animate.css";
import "swiper/css/bundle";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import { global } from "./styles/global";
import Router from "./routes/Router";

const queryClient = new QueryClient();

const container = document.getElementById("root");
const root = createRoot(container);

root.render(
  <QueryClientProvider client={queryClient}>
    <BrowserRouter>
      <Global styles={global} />
      <Router />
    </BrowserRouter>
  </QueryClientProvider>
);
