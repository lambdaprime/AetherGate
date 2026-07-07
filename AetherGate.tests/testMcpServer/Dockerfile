FROM python:3.12-slim

COPY simple-auth /opt/simple-auth

RUN pip install \
  uv

RUN cd /opt/simple-auth && \
  uv sync

RUN useradd --create-home --shell /bin/bash mcp

USER mcp
WORKDIR /opt/simple-auth
